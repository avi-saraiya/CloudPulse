package com.cloudpulse.controller;

import com.cloudpulse.entity.AlertEntity;
import com.cloudpulse.repository.AlertRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class AlertController {

    private final AlertRepository alertRepository;

    public AlertController(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    // GET /api/alerts            -> every alert, newest first
    // GET /api/alerts?status=OPEN -> only alerts with that status
    @GetMapping("/alerts")
    public List<AlertEntity> getAlerts(@RequestParam(required = false) String status) {
        if (status == null) {
            return alertRepository.findAllByOrderByCreatedAtDesc();
        }
        return alertRepository.findByStatusOrderByCreatedAtDesc(status.toUpperCase());
    }
}