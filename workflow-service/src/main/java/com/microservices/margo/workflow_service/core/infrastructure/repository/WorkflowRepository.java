package com.microservices.margo.workflow_service.core.infrastructure.repository;

import com.microservices.margo.workflow_service.core.infrastructure.entity.WorkflowEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@SuppressWarnings("NullableProblems")
@Repository
public interface WorkflowRepository extends JpaRepository<WorkflowEntity, UUID> {
}
