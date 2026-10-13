package com.cloudpulse.repository;

import com.cloudpulse.entity.IncidentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncidentRepository extends JpaRepository<IncidentEntity, Long> {

    // SELECT * FROM incidents ORDER BY created_at DESC  (newest incidents first for the UI)
    List<IncidentEntity> findAllByOrderByCreatedAtDesc();

    // SELECT COUNT(*) FROM incidents WHERE status <> ?  (used for the "open incidents" Prometheus gauge)
    long countByStatusNot(String status);
}