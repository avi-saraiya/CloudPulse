package com.cloudpulse.service;

import com.cloudpulse.entity.AlertEntity;
import com.cloudpulse.entity.IncidentEntity;
import com.cloudpulse.entity.ServiceEntity;
import com.cloudpulse.repository.AlertRepository;
import com.cloudpulse.repository.IncidentRepository;
import com.cloudpulse.repository.MetricRepository;
import com.cloudpulse.repository.ServiceRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests: the repositories are Mockito "mocks" (fake objects), so no database is needed.
 * We tell each mock what to return, call the code under test, then verify what it saved.
 */
class MonitoringServiceTest {

    private ServiceRepository serviceRepository;
    private MetricRepository metricRepository;
    private AlertRepository alertRepository;
    private IncidentRepository incidentRepository;
    private SimulationService simulationService;
    private MonitoringService monitoringService;
    private ServiceEntity payment;

    @BeforeEach
    void setUp() {
        serviceRepository = mock(ServiceRepository.class);
        metricRepository = mock(MetricRepository.class);
        alertRepository = mock(AlertRepository.class);
        incidentRepository = mock(IncidentRepository.class);
        simulationService = new SimulationService(); // real object: it is just a map
        monitoringService = new MonitoringService(serviceRepository, metricRepository, alertRepository,
                incidentRepository, simulationService, new SimpleMeterRegistry());

        payment = new ServiceEntity("Payment API", "HEALTHY");
        when(serviceRepository.findAll()).thenReturn(List.of(payment));
        // By default there is no OPEN alert yet
        when(alertRepository.findByServiceAndRuleNameAndStatus(any(), any(), any())).thenReturn(Optional.empty());
        // save() returns whatever it was given
        when(alertRepository.save(any())).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void normalTelemetryDoesNotCreateAlert() {
        monitoringService.tick();

        verify(alertRepository, never()).save(any());
        verify(incidentRepository, never()).save(any());
        assertThat(payment.getStatus()).isEqualTo("HEALTHY");
    }

    @Test
    void latencySpikeCreatesAlertAndIncident() {
        simulationService.setMode("Payment API", SimulationService.LATENCY_SPIKE);

        monitoringService.tick();

        verify(alertRepository).save(argThat((AlertEntity a) -> a.getRuleName().equals("HIGH_LATENCY")));
        verify(incidentRepository).save(argThat((IncidentEntity i) -> i.getStatus().equals("OPEN")));
        assertThat(payment.getStatus()).isEqualTo("DEGRADED");
    }

    @Test
    void errorSpikeCreatesHighErrorRateAlert() {
        simulationService.setMode("Payment API", SimulationService.ERROR_SPIKE);

        monitoringService.tick();

        verify(alertRepository).save(argThat((AlertEntity a) -> a.getRuleName().equals("HIGH_ERROR_RATE")));
        assertThat(payment.getStatus()).isEqualTo("DEGRADED");
    }

    @Test
    void repeatedBreachDoesNotCreateDuplicateAlert() {
        simulationService.setMode("Payment API", SimulationService.LATENCY_SPIKE);
        // Pretend an OPEN HIGH_LATENCY alert already exists from a previous tick
        AlertEntity existing = new AlertEntity(payment, "HIGH_LATENCY", "OPEN", 1500.0, 1000.0);
        when(alertRepository.findByServiceAndRuleNameAndStatus(payment, "HIGH_LATENCY", "OPEN"))
                .thenReturn(Optional.of(existing));

        monitoringService.tick();
        monitoringService.tick();

        verify(alertRepository, never()).save(any());
        verify(incidentRepository, never()).save(any());
    }

    @Test
    void recoveryResolvesOpenAlert() {
        AlertEntity existing = new AlertEntity(payment, "HIGH_LATENCY", "OPEN", 1500.0, 1000.0);
        when(alertRepository.findByServiceAndRuleNameAndStatus(payment, "HIGH_LATENCY", "OPEN"))
                .thenReturn(Optional.of(existing));

        monitoringService.tick(); // mode is NORMAL, so latency has recovered

        assertThat(existing.getStatus()).isEqualTo("RESOLVED");
        assertThat(existing.getResolvedAt()).isNotNull();
        verify(alertRepository).save(eq(existing));
    }

    @Test
    void downTakesPrecedenceOverDegraded() {
        // Outage has BOTH high latency and a very high error rate: the result must be DOWN, not DEGRADED
        assertThat(MonitoringService.calculateStatus(new MonitoringService.Reading(3000, 0.9))).isEqualTo("DOWN");
        assertThat(MonitoringService.calculateStatus(new MonitoringService.Reading(1500, 0.01))).isEqualTo("DEGRADED");
        assertThat(MonitoringService.calculateStatus(new MonitoringService.Reading(120, 0.01))).isEqualTo("HEALTHY");
    }

    @Test
    void simulationIsDeterministic() {
        // Same mode always produces the same numbers
        assertThat(MonitoringService.readingFor(SimulationService.LATENCY_SPIKE))
                .isEqualTo(MonitoringService.readingFor(SimulationService.LATENCY_SPIKE));

        simulationService.setMode("Payment API", SimulationService.LATENCY_SPIKE);
        simulationService.restoreAll();
        assertThat(simulationService.getMode("Payment API")).isEqualTo(SimulationService.NORMAL);
    }
}
