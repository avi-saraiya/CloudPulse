package com.cloudpulse.service;

import com.cloudpulse.entity.IncidentEntity;
import com.cloudpulse.repository.IncidentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IncidentServiceTest {

    private IncidentRepository incidentRepository;
    private IncidentService incidentService;

    @BeforeEach
    void setUp() {
        incidentRepository = mock(IncidentRepository.class);
        incidentService = new IncidentService(incidentRepository);
        when(incidentRepository.save(any())).thenAnswer(call -> call.getArgument(0));
    }

    private IncidentEntity givenIncident(String status) {
        IncidentEntity incident = new IncidentEntity(null, status);
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));
        return incident;
    }

    @Test
    void acknowledgeRecordsTimestamp() {
        givenIncident("OPEN");

        IncidentEntity result = incidentService.acknowledge(1L);

        assertThat(result.getStatus()).isEqualTo("ACKNOWLEDGED");
        assertThat(result.getAcknowledgedAt()).isNotNull();
    }

    @Test
    void resolveRecordsTimestamp() {
        givenIncident("ACKNOWLEDGED");

        IncidentEntity result = incidentService.resolve(1L);

        assertThat(result.getStatus()).isEqualTo("RESOLVED");
        assertThat(result.getResolvedAt()).isNotNull();
    }

    @Test
    void missingIncidentReturns404() {
        when(incidentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> incidentService.findById(99L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void acknowledgingTwiceReturns409() {
        givenIncident("ACKNOWLEDGED");

        assertThatThrownBy(() -> incidentService.acknowledge(1L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void resolvingResolvedIncidentReturns409() {
        givenIncident("RESOLVED");

        assertThatThrownBy(() -> incidentService.resolve(1L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }
}
