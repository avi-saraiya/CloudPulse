package com.cloudpulse.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Entity representing an operational incident triggered by an alert.
 */
@Entity
@Table(name = "incidents")
public class IncidentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // EAGER: always load the parent row too, so the JSON response can include it
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "alert_id", nullable = false)
    private AlertEntity alert;

    @Column(nullable = false)
    private String status; // e.g., "OPEN", "ACKNOWLEDGED", "RESOLVED"

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "acknowledged_at")
    private OffsetDateTime acknowledgedAt;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    public IncidentEntity() {}

    public IncidentEntity(AlertEntity alert, String status) {
        this.alert = alert;
        this.status = status;
        this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    // Getters
    public Long getId() { return id; }
    public AlertEntity getAlert() { return alert; }
    public String getStatus() { return status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getAcknowledgedAt() { return acknowledgedAt; }
    public OffsetDateTime getResolvedAt() { return resolvedAt; }

    // Setters
    public void setAlert(AlertEntity alert) { this.alert = alert; }
    public void setStatus(String status) { this.status = status; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public void setAcknowledgedAt(OffsetDateTime acknowledgedAt) { this.acknowledgedAt = acknowledgedAt; }
    public void setResolvedAt(OffsetDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
}