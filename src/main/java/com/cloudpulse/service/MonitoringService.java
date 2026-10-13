package com.cloudpulse.service;

import com.cloudpulse.entity.AlertEntity;
import com.cloudpulse.entity.IncidentEntity;
import com.cloudpulse.entity.MetricEntity;
import com.cloudpulse.entity.ServiceEntity;
import com.cloudpulse.repository.AlertRepository;
import com.cloudpulse.repository.IncidentRepository;
import com.cloudpulse.repository.MetricRepository;
import com.cloudpulse.repository.ServiceRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The heart of CloudPulse. Every 5 seconds it:
 *   1. generates one metric reading per service (based on the simulation mode),
 *   2. saves it to the metrics table,
 *   3. checks the two alert rules and opens/resolves alerts (and opens incidents),
 *   4. updates the service status (HEALTHY / DEGRADED / DOWN),
 *   5. publishes the latest values as Prometheus gauges.
 */
@Service
public class MonitoringService {

    // Alert rule names, stored in alerts.rule_name
    public static final String HIGH_LATENCY = "HIGH_LATENCY";
    public static final String HIGH_ERROR_RATE = "HIGH_ERROR_RATE";

    // Thresholds. Latency is in milliseconds; error rate is a fraction (0.10 = 10%).
    public static final double LATENCY_THRESHOLD_MS = 1000;
    public static final double ERROR_RATE_THRESHOLD = 0.10;
    public static final double DOWN_ERROR_RATE = 0.50;

    // A single reading: one latency value and one error-rate value. A record is a tiny immutable class.
    public record Reading(double latencyMs, double errorRate) {}

    private final ServiceRepository serviceRepository;
    private final MetricRepository metricRepository;
    private final AlertRepository alertRepository;
    private final IncidentRepository incidentRepository;
    private final SimulationService simulationService;
    private final MeterRegistry meterRegistry;

    // Latest values per service name. Prometheus gauges read from these maps whenever they are scraped.
    private final Map<String, Double> latestLatency = new ConcurrentHashMap<>();
    private final Map<String, Double> latestErrorRate = new ConcurrentHashMap<>();

    // Spring sees this constructor and passes in ("injects") all the beans it needs.
    public MonitoringService(ServiceRepository serviceRepository, MetricRepository metricRepository,
                             AlertRepository alertRepository, IncidentRepository incidentRepository,
                             SimulationService simulationService, MeterRegistry meterRegistry) {
        this.serviceRepository = serviceRepository;
        this.metricRepository = metricRepository;
        this.alertRepository = alertRepository;
        this.incidentRepository = incidentRepository;
        this.simulationService = simulationService;
        this.meterRegistry = meterRegistry;

        // Gauge = "a value that can go up and down". Shows up as cloudpulse_open_incidents in Prometheus.
        Gauge.builder("cloudpulse.open.incidents", incidentRepository, repo -> repo.countByStatusNot("RESOLVED"))
                .description("Incidents that are OPEN or ACKNOWLEDGED")
                .register(meterRegistry);
    }

    // Runs automatically every 5000 ms (enabled by @EnableScheduling on CloudpulseApplication).
    @Scheduled(fixedRate = 5000)
    public void tick() {
        for (ServiceEntity service : serviceRepository.findAll()) {
            String mode = simulationService.getMode(service.getName());
            Reading reading = readingFor(mode);

            // 1 + 2. Save the reading to the metrics table
            metricRepository.save(new MetricEntity(service, reading.latencyMs(), reading.errorRate()));

            // 3. Check both rules
            checkRule(service, HIGH_LATENCY, reading.latencyMs(), LATENCY_THRESHOLD_MS);
            checkRule(service, HIGH_ERROR_RATE, reading.errorRate(), ERROR_RATE_THRESHOLD);

            // 4. Update service status
            service.setStatus(calculateStatus(reading));
            serviceRepository.save(service);

            // 5. Publish to Prometheus
            publishGauges(service.getName(), reading);
        }
    }

    // Fixed numbers for each mode, no randomness, so the demo and the tests behave the same every time.
    public static Reading readingFor(String mode) {
        return switch (mode) {
            case SimulationService.LATENCY_SPIKE -> new Reading(1500, 0.01);
            case SimulationService.ERROR_SPIKE -> new Reading(120, 0.25);
            case SimulationService.OUTAGE -> new Reading(3000, 0.90);
            default -> new Reading(120, 0.01); // NORMAL
        };
    }

    // DOWN is checked first, so it wins ("takes precedence") over DEGRADED.
    public static String calculateStatus(Reading reading) {
        if (reading.errorRate() >= DOWN_ERROR_RATE) {
            return "DOWN";
        }
        if (reading.latencyMs() > LATENCY_THRESHOLD_MS || reading.errorRate() > ERROR_RATE_THRESHOLD) {
            return "DEGRADED";
        }
        return "HEALTHY";
    }

    /**
     * Opens an alert + incident when a rule is first breached, and resolves the alert once the value recovers.
     * Because we look for an existing OPEN alert first, a breach that lasts many ticks creates only ONE alert.
     */
    void checkRule(ServiceEntity service, String ruleName, double observed, double threshold) {
        boolean breached = observed > threshold;
        Optional<AlertEntity> openAlert = alertRepository.findByServiceAndRuleNameAndStatus(service, ruleName, "OPEN");

        if (breached && openAlert.isEmpty()) {
            AlertEntity alert = alertRepository.save(new AlertEntity(service, ruleName, "OPEN", observed, threshold));
            incidentRepository.save(new IncidentEntity(alert, "OPEN"));
        } else if (!breached && openAlert.isPresent()) {
            AlertEntity alert = openAlert.get();
            alert.setStatus("RESOLVED");
            alert.setResolvedAt(OffsetDateTime.now(ZoneOffset.UTC));
            alertRepository.save(alert);
            // The incident is NOT auto-resolved: a human confirms recovery and resolves it in the UI.
        }
    }

    private void publishGauges(String serviceName, Reading reading) {
        // Register each gauge once per service (computeIfAbsent only runs the lambda the first time).
        // "service" is a label with only 3 possible values, so it is safe for Prometheus (low cardinality).
        latestLatency.computeIfAbsent(serviceName, name -> {
            Gauge.builder("cloudpulse.service.latency.ms", latestLatency, m -> m.getOrDefault(name, 0.0))
                    .tag("service", name).description("Latest simulated latency in ms").register(meterRegistry);
            return 0.0;
        });
        latestErrorRate.computeIfAbsent(serviceName, name -> {
            Gauge.builder("cloudpulse.service.error.rate", latestErrorRate, m -> m.getOrDefault(name, 0.0))
                    .tag("service", name).description("Latest simulated error rate (0-1)").register(meterRegistry);
            return 0.0;
        });
        latestLatency.put(serviceName, reading.latencyMs());
        latestErrorRate.put(serviceName, reading.errorRate());
    }
}
