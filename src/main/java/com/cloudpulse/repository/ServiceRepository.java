package com.cloudpulse.repository;

import com.cloudpulse.entity.ServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ServiceRepository extends JpaRepository<ServiceEntity, Long> {

    // SELECT * FROM services WHERE name = ?  (the simulation buttons pick a service by name)
    Optional<ServiceEntity> findByName(String name);
}