package com.cloudpulse.controller;

import com.cloudpulse.entity.IncidentEntity;
import com.cloudpulse.service.IncidentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class IncidentController {

    // The controller only translates HTTP <-> Java; the lifecycle rules live in IncidentService.
    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    // GET /api/incidents -> all incidents, newest first
    @GetMapping("/incidents")
    public List<IncidentEntity> getAllIncidents() {
        return incidentService.findAll();
    }

    // GET /api/incidents/5 -> one incident, or 404
    @GetMapping("/incidents/{id}")
    public IncidentEntity getIncident(@PathVariable Long id) {
        return incidentService.findById(id);
    }

    // POST /api/incidents/5/acknowledge -> OPEN becomes ACKNOWLEDGED (409 otherwise)
    @PostMapping("/incidents/{id}/acknowledge")
    public IncidentEntity acknowledge(@PathVariable Long id) {
        return incidentService.acknowledge(id);
    }

    // POST /api/incidents/5/resolve -> OPEN/ACKNOWLEDGED becomes RESOLVED (409 if already resolved)
    @PostMapping("/incidents/{id}/resolve")
    public IncidentEntity resolve(@PathVariable Long id) {
        return incidentService.resolve(id);
    }
}