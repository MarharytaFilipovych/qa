package com.microservices.margo.workflow_service.core.domain;

public enum WorkflowState {
    STARTED,
    ORDER_CREATED,
    ORDER_CONFIRMED,
    COMPLETED,
    COMPENSATING,
    COMPENSATED,
    FAILED
}