package com.cloudpulse.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

// A breached rule (HIGH_LATENCY or HIGH_ERROR_RATE) for one service. Status is OPEN or RESOLVED.
@Entity
@Table(name = "alerts")
public class AlertEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @ManyToOne
    public ServiceEntity service;
    public String ruleName;
    public String status = "OPEN";
    public OffsetDateTime createdAt = OffsetDateTime.now();

    public AlertEntity() {}

    public AlertEntity(ServiceEntity service, String ruleName) {
        this.service = service;
        this.ruleName = ruleName;
    }
}
