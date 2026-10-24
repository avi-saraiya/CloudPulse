package com.cloudpulse.repository;

import com.cloudpulse.entity.AlertEntity;
import com.cloudpulse.entity.ServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<AlertEntity, Long> {

    // The OPEN alert for this service and rule, or null if there is none
    AlertEntity findByServiceAndRuleNameAndStatus(ServiceEntity service, String ruleName, String status);
}
