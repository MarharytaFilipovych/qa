package com.microservices.margo.workflow_service.data;

import com.microservices.margo.workflow_service.core.application.request.CreateOrderRequest;
import com.microservices.margo.workflow_service.core.domain.Workflow;
import com.microservices.margo.workflow_service.core.domain.WorkflowState;
import com.microservices.margo.workflow_service.core.domain.WorkflowType;
import com.microservices.margo.workflow_service.core.infrastructure.entity.WorkflowEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public final class WorkflowData {

    private static final UUID WORKFLOW_ID = UUID.randomUUID();
    private static final UUID OWNER_USER_ID = UUID.randomUUID();
    private static final LocalDateTime CREATED_AT = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    private static final LocalDateTime UPDATED_AT = CREATED_AT.plusSeconds(1);
    private static final String PAYLOAD = "CreateOrderRequest[ownerUserId=" + OWNER_USER_ID + ", itemName=Latte, quantity=2, price=5.99]";

    public static CreateOrderRequest createOrderRequest() {
        return new CreateOrderRequest(OWNER_USER_ID, "Latte", 2, new BigDecimal("5.99"));
    }

    public static Workflow getWorkflow() {
        return Workflow.builder()
                .id(WORKFLOW_ID)
                .type("CREATE_ORDER")
                .state(WorkflowState.STARTED)
                .payload(PAYLOAD)
                .createdAt(CREATED_AT)
                .updatedAt(UPDATED_AT)
                .build();
    }

    public static WorkflowEntity getWorkflowEntity() {
        return WorkflowEntity.builder()
                .id(WORKFLOW_ID)
                .type("CREATE_ORDER")
                .state(WorkflowState.STARTED)
                .payload(PAYLOAD)
                .createdAt(CREATED_AT)
                .updatedAt(UPDATED_AT)
                .build();
    }

    public static Workflow getStartedWorkflow() {
        return Workflow.builder()
                .type(WorkflowType.CREATE_ORDER.toString())
                .state(WorkflowState.STARTED)
                .payload(createOrderRequest().toString())
                .build();
    }

    public static WorkflowEntity getStartedEntity() {
        return WorkflowEntity.builder()
                .type(WorkflowType.CREATE_ORDER.toString())
                .state(WorkflowState.STARTED)
                .payload(createOrderRequest().toString())
                .build();
    }
}