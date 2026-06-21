package com.microservices.margo.workflow_service.core.domain;

import lombok.Builder;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder(toBuilder = true)
public record Workflow(
        UUID id,
        String type,
        WorkflowState state,
        String payload,
        String lastError,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public Workflow transitionTo(WorkflowState next) {
        return this.toBuilder()
                .state(next)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public Workflow fail(String error) {
        return this.toBuilder()
                .state(WorkflowState.FAILED)
                .lastError(error)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public Workflow compensating(String error) {
        return this.toBuilder()
                .state(WorkflowState.COMPENSATING)
                .lastError(error)
                .updatedAt(LocalDateTime.now())
                .build();
    }
}