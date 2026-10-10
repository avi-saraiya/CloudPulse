package com.cloudpulse.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "metrics")
public class MetricEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceEntity service;

    @Column(name = "metric_type", nullable = false)
    private String metricType;

    @Column(nullable = false)
    private Double value;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private OffsetDateTime recordedAt;

    public MetricEntity() {}

    public MetricEntity(ServiceEntity service, String metricType, Double value) {
        this.service = service;
        this.metricType = metricType;
        this.value = value;
        this.recordedAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public ServiceEntity getService() { return service; }
    public String getMetricType() { return metricType; }
    public Double getValue() { return value; }
    public OffsetDateTime getRecordedAt() { return recordedAt; }

    public void setService(ServiceEntity service) { this.service = service; }
    public void setMetricType(String metricType) { this.metricType = metricType; }
    public void setValue(Double value) { this.value = value; }
}