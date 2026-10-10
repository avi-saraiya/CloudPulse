package com.cloudpulse.controller;

import com.cloudpulse.entity.AlertEntity;
import com.cloudpulse.repository.AlertRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class AlertController {

    private final AlertRepository alertRepository;

    public AlertController(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    @GetMapping("/alerts")
    public List<AlertEntity> getAllServices() {
        return alertRepository.findAll();
    }
}