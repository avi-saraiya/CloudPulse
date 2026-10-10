package com.cloudpulse.controller;

import com.cloudpulse.entity.MetricEntity;
import com.cloudpulse.repository.MetricRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class MetricController {

    private final MetricRepository metricRepository;

    public MetricController(MetricRepository metricRepository) {
        this.metricRepository = metricRepository;
    }

    @GetMapping("/metrics")
    public List<MetricEntity> getAllServices() {
        return metricRepository.findAll();
    }
}