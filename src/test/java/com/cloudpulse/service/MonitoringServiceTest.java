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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// The repositories are Mockito mocks (fake objects), so no database is needed.
class MonitoringServiceTest {

    private AlertRepository alertRepository;
    private IncidentRepository incidentRepository;
    private MonitoringService monitoringService;
    private ServiceEntity payment;

    @BeforeEach
    void setUp() {
        ServiceRepository serviceRepository = mock(ServiceRepository.class);
        alertRepository = mock(AlertRepository.class);
        incidentRepository = mock(IncidentRepository.class);
        monitoringService = new MonitoringService(serviceRepository, mock(MetricRepository.class),
                alertRepository, incidentRepository, new SimpleMeterRegistry());

        payment = new ServiceEntity();
        payment.name = "Payment API";
        when(serviceRepository.findAll()).thenReturn(List.of(payment));
    }

    @Test
    void latencySpikeOpensAlertAndIncident() {
        monitoringService.modes.put("Payment API", "latency-spike");

        monitoringService.tick();

        verify(alertRepository).save(any(AlertEntity.class));
        verify(incidentRepository).save(any(IncidentEntity.class));
        assertThat(payment.status).isEqualTo("DEGRADED");
    }

    @Test
    void ongoingBreachDoesNotOpenSecondAlert() {
        monitoringService.modes.put("Payment API", "latency-spike");
        AlertEntity open = new AlertEntity(payment, "HIGH_LATENCY");
        when(alertRepository.findByServiceAndRuleNameAndStatus(payment, "HIGH_LATENCY", "OPEN")).thenReturn(open);

        monitoringService.tick();

        verify(alertRepository, never()).save(any());
        verify(incidentRepository, never()).save(any());
    }

    @Test
    void recoveryResolvesAlert() {
        AlertEntity open = new AlertEntity(payment, "HIGH_LATENCY");
        when(alertRepository.findByServiceAndRuleNameAndStatus(payment, "HIGH_LATENCY", "OPEN")).thenReturn(open);

        monitoringService.tick(); // no mode set, so latency is back to normal

        assertThat(open.status).isEqualTo("RESOLVED");
        assertThat(payment.status).isEqualTo("HEALTHY");
    }
}
