package com.cloudpulse.controller;

import com.cloudpulse.entity.MetricEntity;
import com.cloudpulse.repository.MetricRepository;
import com.cloudpulse.repository.ServiceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api")
public class MetricController {

    private final MetricRepository metricRepository;
    private final ServiceRepository serviceRepository;

    public MetricController(MetricRepository metricRepository, ServiceRepository serviceRepository) {
        this.metricRepository = metricRepository;
        this.serviceRepository = serviceRepository;
    }

    // GET /api/services/1/metrics -> the 50 most recent readings for that service (newest first)
    @GetMapping("/services/{id}/metrics")
    public List<MetricEntity> getRecentMetrics(@PathVariable Long id) {
        if (!serviceRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Service " + id + " not found");
        }
        return metricRepository.findTop50ByServiceIdOrderByRecordedAtDesc(id);
    }
}