package com.cloudpulse.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

// One reading for one service. Field names in camelCase map to snake_case columns (latencyMs -> latency_ms).
@Entity
@Table(name = "metrics")
public class MetricEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @ManyToOne // stored as the service_id column
    public ServiceEntity service;
    public double latencyMs;
    public double errorRate; // a fraction: 0.05 = 5%
    public OffsetDateTime recordedAt = OffsetDateTime.now();

    public MetricEntity() {} // Hibernate needs an empty constructor

    public MetricEntity(ServiceEntity service, double latencyMs, double errorRate) {
        this.service = service;
        this.latencyMs = latencyMs;
        this.errorRate = errorRate;
    }
}
