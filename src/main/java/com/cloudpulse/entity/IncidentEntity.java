package com.cloudpulse.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "incidents")
public class IncidentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alert_id", nullable = false)
    private AlertEntity alert;

    @Column(nullable = false)
    private String status; // e.g., "OPEN", "RESOLVED"

    @Column(name = "triggered_at", nullable = false, updatable = false)
    private OffsetDateTime triggeredAt;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    public IncidentEntity() {}

    public IncidentEntity(AlertEntity alert, String status) {
        this.alert = alert;
        this.status = status;
        this.triggeredAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public AlertEntity getAlert() { return alert; }
    public String getStatus() { return status; }
    public OffsetDateTime getTriggeredAt() { return triggeredAt; }
    public OffsetDateTime getResolvedAt() { return resolvedAt; }

    public void setAlert(AlertEntity alert) { this.alert = alert; }
    public void setStatus(String status) { this.status = status; }
    public void setResolvedAt(OffsetDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
}