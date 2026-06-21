package com.microservices.margo.workflow_service.core.application.usecase;

import com.microservices.margo.workflow_service.core.application.mapper.WorkflowMapper;
import com.microservices.margo.workflow_service.core.domain.Workflow;
import com.microservices.margo.workflow_service.core.infrastructure.repository.WorkflowRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetWorkflowUseCase {

    private final WorkflowRepository repository;
    private final WorkflowMapper mapper;

    public Workflow execute(UUID workflowId) {
        return repository.findById(workflowId)
                .map(mapper::toDomain)
                .orElseThrow(() -> new EntityNotFoundException("Workflow not found: " + workflowId));
    }
}