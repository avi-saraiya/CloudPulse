package com.cloudpulse.service;

import com.cloudpulse.entity.AlertEntity;
import com.cloudpulse.entity.IncidentEntity;
import com.cloudpulse.entity.MetricEntity;
import com.cloudpulse.entity.ServiceEntity;
import com.cloudpulse.repository.AlertRepository;
import com.cloudpulse.repository.IncidentRepository;
import com.cloudpulse.repository.MetricRepository;
import com.cloudpulse.repository.ServiceRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Every 5 seconds: generate a reading per service, save it, check the alert rules, update the status,
// and publish the reading to Prometheus.
@Service
public class MonitoringService {

    // Service name -> failure mode ("latency-spike", "error-spike", "outage"). Missing means normal.
    // Concurrent because web requests and the scheduler use it from different threads.
    public final Map<String, String> modes = new ConcurrentHashMap<>();

    // Latest values per service name, read by the Prometheus gauges
    private final Map<String, Double> latencies = new ConcurrentHashMap<>();
    private final Map<String, Double> errorRates = new ConcurrentHashMap<>();

    private final ServiceRepository serviceRepository;
    private final MetricRepository metricRepository;
    private final AlertRepository alertRepository;
    private final IncidentRepository incidentRepository;
    private final MeterRegistry meterRegistry;

    public MonitoringService(ServiceRepository serviceRepository, MetricRepository metricRepository,
                             AlertRepository alertRepository, IncidentRepository incidentRepository,
                             MeterRegistry meterRegistry) {
        this.serviceRepository = serviceRepository;
        this.metricRepository = metricRepository;
        this.alertRepository = alertRepository;
        this.incidentRepository = incidentRepository;
        this.meterRegistry = meterRegistry;
    }

    @Scheduled(fixedRate = 5000)
    public void tick() {
        for (ServiceEntity service : serviceRepository.findAll()) {
            String mode = modes.getOrDefault(service.name, "normal");
            double latency = mode.equals("latency-spike") ? 1500 : mode.equals("outage") ? 3000 : 120;
            double errorRate = mode.equals("error-spike") ? 0.25 : mode.equals("outage") ? 0.90 : 0.01;

            metricRepository.save(new MetricEntity(service, latency, errorRate));

            checkRule(service, "HIGH_LATENCY", latency > 1000);
            checkRule(service, "HIGH_ERROR_RATE", errorRate > 0.10);

            service.status = errorRate >= 0.50 ? "DOWN" : latency > 1000 || errorRate > 0.10 ? "DEGRADED" : "HEALTHY";
            serviceRepository.save(service);

            // Shows up in Prometheus as cloudpulse_service_latency_ms{service="..."}.
            // put() returns null the first time a name is added; that is when the gauges are registered.
            String name = service.name;
            errorRates.put(name, errorRate);
            if (latencies.put(name, latency) == null) {
                meterRegistry.gauge("cloudpulse.service.latency.ms", Tags.of("service", name), latencies, m -> m.get(name));
                meterRegistry.gauge("cloudpulse.service.error.rate", Tags.of("service", name), errorRates, m -> m.get(name));
            }
        }
    }

    // Breached with no OPEN alert -> open an alert and an incident. Recovered -> resolve the alert.
    // The incident stays open until the operator resolves it.
    void checkRule(ServiceEntity service, String ruleName, boolean breached) {
        AlertEntity open = alertRepository.findByServiceAndRuleNameAndStatus(service, ruleName, "OPEN");
        if (breached && open == null) {
            AlertEntity alert = alertRepository.save(new AlertEntity(service, ruleName));
            incidentRepository.save(new IncidentEntity(alert));
        } else if (!breached && open != null) {
            open.status = "RESOLVED";
            alertRepository.save(open);
        }
    }
}
