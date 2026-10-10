package com.cloudpulse.controller;

import com.cloudpulse.entity.IncidentEntity;
import com.cloudpulse.repository.IncidentRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class IncidentController {

    private final IncidentRepository incidentRepository;

    public IncidentController(IncidentRepository incidentRepository) {
        this.incidentRepository = incidentRepository;
    }

    @GetMapping("/incidents")
    public List<IncidentEntity> getAllServices() {
        return incidentRepository.findAll();
    }
}