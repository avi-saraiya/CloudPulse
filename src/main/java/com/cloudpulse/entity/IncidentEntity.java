package com.cloudpulse.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

// Opened together with an alert; the operator moves it OPEN -> ACKNOWLEDGED -> RESOLVED in the UI.
@Entity
@Table(name = "incidents")
public class IncidentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @ManyToOne
    public AlertEntity alert;
    public String status = "OPEN";
    public OffsetDateTime createdAt = OffsetDateTime.now();
    public OffsetDateTime acknowledgedAt;
    public OffsetDateTime resolvedAt;

    public IncidentEntity() {}

    public IncidentEntity(AlertEntity alert) {
        this.alert = alert;
    }
}
