package com.cloudpulse.repository;

import com.cloudpulse.entity.ServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

// JpaRepository already provides findAll, findById and save
public interface ServiceRepository extends JpaRepository<ServiceEntity, Long> {
}
