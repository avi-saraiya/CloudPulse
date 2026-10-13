package com.cloudpulse.repository;

import com.cloudpulse.entity.MetricEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MetricRepository extends JpaRepository<MetricEntity, Long> {

    // SELECT * FROM metrics WHERE service_id = ? ORDER BY recorded_at DESC LIMIT 50
    // "Top50" limits the result so the API never returns the whole (ever-growing) table.
    List<MetricEntity> findTop50ByServiceIdOrderByRecordedAtDesc(Long serviceId);
}