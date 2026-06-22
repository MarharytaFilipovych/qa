package com.microservices.margo.workflow_service.core.application.usecase;

import com.microservices.margo.workflow_service.core.application.mapper.WorkflowMapper;
import com.microservices.margo.workflow_service.core.application.request.CreateOrderRequest;
import com.microservices.margo.workflow_service.core.domain.Workflow;
import com.microservices.margo.workflow_service.core.domain.WorkflowState;
import com.microservices.margo.workflow_service.core.infrastructure.client.OrderServiceClient;
import com.microservices.margo.workflow_service.core.infrastructure.entity.WorkflowEntity;
import com.microservices.margo.workflow_service.core.infrastructure.repository.WorkflowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StartCreateOrderWorkflowUseCaseTest {

    @Mock WorkflowRepository repository;
    @Mock WorkflowMapper mapper;
    @Mock OrderServiceClient orderServiceClient;

    @InjectMocks StartCreateOrderWorkflowUseCase useCase;

    private CreateOrderRequest request;

    @BeforeEach
    void setUp() {
        request = new CreateOrderRequest(UUID.randomUUID(), "Latte", 2, BigDecimal.valueOf(5.99));
    }

    /**
     * Simulates the repository round-trip: mapper.toEntity → repository.save → mapper.toDomain.
     * Each saved entity gets its own UUID so the workflow id persists across transitions.
     */
    private void configureSaveRoundTrip() {
        when(mapper.toEntity(any(Workflow.class))).thenAnswer(inv -> {
            Workflow w = inv.getArgument(0);
            WorkflowEntity e = new WorkflowEntity();
            e.setId(w.id() != null ? w.id() : UUID.randomUUID());
            e.setState(w.state());
            return e;
        });
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toDomain(any(WorkflowEntity.class))).thenAnswer(inv -> {
            WorkflowEntity e = inv.getArgument(0);
            return Workflow.builder()
                    .id(e.getId())
                    .state(e.getState())
                    .build();
        });
    }

    @Test
    void execute_happyPath_returnsCompleted() {
        configureSaveRoundTrip();
        UUID orderId = UUID.randomUUID();
        when(orderServiceClient.createOrder(request)).thenReturn(orderId);

        Workflow result = useCase.execute(request);

        assertThat(result.state()).isEqualTo(WorkflowState.COMPLETED);
        verify(orderServiceClient).createOrder(request);
        verify(orderServiceClient).confirmOrder(orderId);
    }

    @Test
    void execute_whenCreateOrderFails_returnsFailed() {
        configureSaveRoundTrip();
        when(orderServiceClient.createOrder(any())).thenThrow(new RuntimeException("order-service down"));

        Workflow result = useCase.execute(request);

        assertThat(result.state()).isEqualTo(WorkflowState.FAILED);
        verify(orderServiceClient, never()).confirmOrder(any());
        verify(orderServiceClient, never()).cancelOrder(any());
    }

    @Test
    void execute_whenConfirmFails_andCancelSucceeds_returnsCompensated() {
        configureSaveRoundTrip();
        UUID orderId = UUID.randomUUID();
        when(orderServiceClient.createOrder(request)).thenReturn(orderId);
        doThrow(new RuntimeException("confirm failed")).when(orderServiceClient).confirmOrder(orderId);

        Workflow result = useCase.execute(request);

        assertThat(result.state()).isEqualTo(WorkflowState.COMPENSATED);
        verify(orderServiceClient).cancelOrder(orderId);
    }

    @Test
    void execute_whenConfirmAndCancelBothFail_returnsFailed() {
        configureSaveRoundTrip();
        UUID orderId = UUID.randomUUID();
        when(orderServiceClient.createOrder(request)).thenReturn(orderId);
        doThrow(new RuntimeException("confirm failed")).when(orderServiceClient).confirmOrder(orderId);
        doThrow(new RuntimeException("cancel failed")).when(orderServiceClient).cancelOrder(orderId);

        Workflow result = useCase.execute(request);

        assertThat(result.state()).isEqualTo(WorkflowState.FAILED);
    }
}