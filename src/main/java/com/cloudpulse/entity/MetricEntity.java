package com.cloudpulse.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * Entity representing a metric observation recorded for a service.
 * Note: error_rate is stored strictly as a fraction (e.g., 0.05 represents 5%).
 */
@Entity
@Table(name = "metrics")
public class MetricEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceEntity service;

    @Column(name = "latency_ms", nullable = false)
    private Double latencyMs;

    /**
     * Stored as a fraction between 0.00 and 1.00 (e.g., 0.05 = 5%).
     */
    @Column(name = "error_rate", nullable = false)
    private Double errorRate;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private OffsetDateTime recordedAt;

    public MetricEntity() {}

    public MetricEntity(ServiceEntity service, Double latencyMs, Double errorRate) {
        this.service = service;
        this.latencyMs = latencyMs;
        this.errorRate = errorRate;
        this.recordedAt = OffsetDateTime.now();
    }

    // Getters
    public Long getId() { return id; }
    public ServiceEntity getService() { return service; }
    public Double getLatencyMs() { return latencyMs; }
    public Double getErrorRate() { return errorRate; }
    public OffsetDateTime getRecordedAt() { return recordedAt; }

    // Setters
    public void setService(ServiceEntity service) { this.service = service; }
    public void setLatencyMs(Double latencyMs) { this.latencyMs = latencyMs; }
    public void setErrorRate(Double errorRate) { this.errorRate = errorRate; }
    public void setRecordedAt(OffsetDateTime recordedAt) { this.recordedAt = recordedAt; }
}