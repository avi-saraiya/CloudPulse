package com.cloudpulse.repository;

import com.cloudpulse.entity.IncidentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentRepository extends JpaRepository<IncidentEntity, Long> {
}
