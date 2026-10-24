package com.cloudpulse.repository;

import com.cloudpulse.entity.MetricEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MetricRepository extends JpaRepository<MetricEntity, Long> {

    // Spring builds the SQL from the name: newest metric (highest id) for one service
    MetricEntity findFirstByServiceIdOrderByIdDesc(Long serviceId);
}
