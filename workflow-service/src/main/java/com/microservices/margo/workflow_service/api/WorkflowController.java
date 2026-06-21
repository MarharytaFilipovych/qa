package com.microservices.margo.workflow_service.api;

import com.microservices.margo.workflow_service.core.application.request.CreateOrderRequest;
import com.microservices.margo.workflow_service.core.application.usecase.GetWorkflowUseCase;
import com.microservices.margo.workflow_service.core.application.usecase.StartCreateOrderWorkflowUseCase;
import com.microservices.margo.workflow_service.core.domain.Workflow;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/workflows")
@RequiredArgsConstructor
public class WorkflowController {
    private final StartCreateOrderWorkflowUseCase startPlaceOrderWorkflow;
    private final GetWorkflowUseCase getWorkflow;

    @PostMapping("/create-order")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Workflow startPlaceOrder(@Valid @RequestBody CreateOrderRequest request) {
        return startPlaceOrderWorkflow.execute(request);
    }

    @GetMapping("/{workflowId}")
    public Workflow getById(@PathVariable UUID workflowId) {
        return getWorkflow.execute(workflowId);
    }
}
