// Works like SQLAlchemy, but for Java instead

package com.cloudpulse.entity;

// imports java persistent API to define methods to query the database
import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "services")
public class ServiceEntity {
    // Defines id as the primary key for the services schema
    @Id
    //Defines id's values to be sequential (BIGSERIAL equivalent)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    // Need this contructor here cause certain frameworks need an unparameterized one
    public ServiceEntity() {}

    public ServiceEntity(String name, String status) {
        this.name = name;
        this.status = status;
        this.createdAt = OffsetDateTime.now();
    }
    // Getters methods
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getStatus() { return status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    // Setter methods
    public void setStatus(String status) { this.status = status; }
}