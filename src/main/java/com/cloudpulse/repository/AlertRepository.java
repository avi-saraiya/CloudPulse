package com.cloudpulse.repository;

import com.cloudpulse.entity.AlertEntity;
import com.cloudpulse.entity.ServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlertRepository extends JpaRepository<AlertEntity, Long> {

    // Spring Data builds the SQL from the method name:
    // SELECT * FROM alerts WHERE service_id = ? AND rule_name = ? AND status = ?
    // Used to check "is there already an OPEN alert for this service and rule?" so we never create duplicates.
    Optional<AlertEntity> findByServiceAndRuleNameAndStatus(ServiceEntity service, String ruleName, String status);

    // SELECT * FROM alerts WHERE status = ? ORDER BY created_at DESC
    List<AlertEntity> findByStatusOrderByCreatedAtDesc(String status);

    // SELECT * FROM alerts ORDER BY created_at DESC
    List<AlertEntity> findAllByOrderByCreatedAtDesc();
}