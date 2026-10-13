package com.cloudpulse.service;

import com.cloudpulse.entity.IncidentEntity;
import com.cloudpulse.repository.IncidentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Incident lifecycle: OPEN -> ACKNOWLEDGED -> RESOLVED.
 *
 * Policy for invalid transitions (documented in PROJECT_INFO.md):
 *   - acknowledge is only allowed from OPEN
 *   - resolve is allowed from OPEN or ACKNOWLEDGED (you may skip acknowledging)
 *   - anything else returns 409 Conflict
 */
@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;

    public IncidentService(IncidentRepository incidentRepository) {
        this.incidentRepository = incidentRepository;
    }

    public List<IncidentEntity> findAll() {
        return incidentRepository.findAllByOrderByCreatedAtDesc();
    }

    // ResponseStatusException makes Spring answer the HTTP request with that status code (here 404).
    public IncidentEntity findById(Long id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident " + id + " not found"));
    }

    public IncidentEntity acknowledge(Long id) {
        IncidentEntity incident = findById(id);
        if (!"OPEN".equals(incident.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only OPEN incidents can be acknowledged (current status: " + incident.getStatus() + ")");
        }
        incident.setStatus("ACKNOWLEDGED");
        incident.setAcknowledgedAt(OffsetDateTime.now(ZoneOffset.UTC));
        return incidentRepository.save(incident);
    }

    public IncidentEntity resolve(Long id) {
        IncidentEntity incident = findById(id);
        if ("RESOLVED".equals(incident.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Incident " + id + " is already RESOLVED");
        }
        incident.setStatus("RESOLVED");
        incident.setResolvedAt(OffsetDateTime.now(ZoneOffset.UTC));
        return incidentRepository.save(incident);
    }
}
