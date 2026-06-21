package com.microservices.margo.workflow_service.core.application.usecase;

import com.microservices.margo.workflow_service.core.application.mapper.WorkflowMapper;
import com.microservices.margo.workflow_service.core.application.request.CreateOrderRequest;
import com.microservices.margo.workflow_service.core.domain.Workflow;
import com.microservices.margo.workflow_service.core.domain.WorkflowState;
import com.microservices.margo.workflow_service.core.domain.WorkflowType;
import com.microservices.margo.workflow_service.core.infrastructure.client.OrderServiceClient;
import com.microservices.margo.workflow_service.core.infrastructure.repository.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StartCreateOrderWorkflowUseCase {

    private final WorkflowRepository repository;
    private final WorkflowMapper mapper;
    private final OrderServiceClient orderServiceClient;

    public Workflow execute(CreateOrderRequest request) {
        Workflow workflow = createWorkflow(request);

        workflow = save(workflow);
        log.info("Workflow {} started", workflow.id());

        UUID orderId;
        try {
            orderId = orderServiceClient.createOrder(request);
            workflow = save(workflow.transitionTo(WorkflowState.ORDER_CREATED));
            log.info("Workflow {} order created: {}", workflow.id(), orderId);
        } catch (Exception e) {
            logWorkflowFailure(WorkflowState.ORDER_CREATED, workflow.id(), e.getMessage());
            return save(workflow.fail("Order creation failed: " + e.getMessage()));
        }

        workflow = confirmOrder(workflow, orderId);

        if (workflow.state() != WorkflowState.ORDER_CONFIRMED) {
            return workflow;
        }

        return save(workflow.transitionTo(WorkflowState.COMPLETED));
    }

    private Workflow createWorkflow(CreateOrderRequest request){
        return Workflow.builder()
                .type(WorkflowType.CREATE_ORDER.toString())
                .state(WorkflowState.STARTED)
                .payload(request.toString())
                .build();
    }

    private Workflow confirmOrder(Workflow workflow, UUID orderId){
        try {
            orderServiceClient.confirmOrder(orderId);
            workflow = save(workflow.transitionTo(WorkflowState.ORDER_CONFIRMED));
            log.info("Workflow {} order confirmed: {}", workflow.id(), orderId);
            return workflow;
        } catch (Exception e) {
            logWorkflowFailure(WorkflowState.ORDER_CONFIRMED, workflow.id(), e.getMessage());
            workflow = save(workflow.compensating("Order confirmation failed: " + e.getMessage()));
            return cancelOrder(workflow, orderId);
        }
    }

    private Workflow cancelOrder(Workflow workflow, UUID orderId){
        try {
            orderServiceClient.cancelOrder(orderId);
            workflow = save(workflow.transitionTo(WorkflowState.COMPENSATED));
            log.info("Workflow {} compensated: order {} cancelled", workflow.id(), orderId);
            return workflow;
        } catch (Exception e) {
            logWorkflowFailure(WorkflowState.COMPENSATED, workflow.id(), e.getMessage());
            return save(workflow.fail(workflow.lastError() + " | Compensation failed: " + e.getMessage()));
        }
    }

    private Workflow save(Workflow instance) {
        return mapper.toDomain(repository.save(mapper.toEntity(instance)));
    }

    private void logWorkflowFailure(WorkflowState state, UUID id, String error){
        log.error("Workflow {} failed at {}, compensating: {}", state, id, error);

    }
}