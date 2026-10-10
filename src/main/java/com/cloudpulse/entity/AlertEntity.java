package com.cloudpulse.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "alerts")
public class AlertEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceEntity service;

    @Column(name = "metric_type", nullable = false)
    private String metricType;

    @Column(nullable = false)
    private Double threshold;

    @Column(nullable = false)
    private String condition; // e.g., "GREATER_THAN", "LESS_THAN"

    @Column(nullable = false)
    private String status; // e.g., "OK", "FIRING"

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public AlertEntity() {}

    public AlertEntity(ServiceEntity service, String metricType, Double threshold, String condition, String status) {
        this.service = service;
        this.metricType = metricType;
        this.threshold = threshold;
        this.condition = condition;
        this.status = status;
        this.createdAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public ServiceEntity getService() { return service; }
    public String getMetricType() { return metricType; }
    public Double getThreshold() { return threshold; }
    public String getCondition() { return condition; }
    public String getStatus() { return status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    public void setService(ServiceEntity service) { this.service = service; }
    public void setMetricType(String metricType) { this.metricType = metricType; }
    public void setThreshold(Double threshold) { this.threshold = threshold; }
    public void setCondition(String condition) { this.condition = condition; }
    public void setStatus(String status) { this.status = status; }
}