package com.cloudpulse.entity;

import jakarta.persistence.*;

// One row of the services table. Hibernate maps each field to the column with the same name.
@Entity
@Table(name = "services")
public class ServiceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // the database picks the id (BIGSERIAL)
    public Long id;
    public String name;
    public String status;
}
