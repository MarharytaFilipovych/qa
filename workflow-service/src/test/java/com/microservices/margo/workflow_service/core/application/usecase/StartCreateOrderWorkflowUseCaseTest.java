package com.microservices.margo.workflow_service.core.application.usecase;

import com.microservices.margo.workflow_service.core.application.mapper.WorkflowMapper;
import com.microservices.margo.workflow_service.core.application.request.CreateOrderRequest;
import com.microservices.margo.workflow_service.core.domain.Workflow;
import com.microservices.margo.workflow_service.core.domain.WorkflowState;
import com.microservices.margo.workflow_service.core.infrastructure.client.OrderServiceClient;
import com.microservices.margo.workflow_service.core.infrastructure.entity.WorkflowEntity;
import com.microservices.margo.workflow_service.core.infrastructure.repository.WorkflowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static com.microservices.margo.workflow_service.data.WorkflowData.createOrderRequest;
import static com.microservices.margo.workflow_service.data.WorkflowData.getWorkflow;
import static com.microservices.margo.workflow_service.data.WorkflowData.getWorkflowEntity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("StartCreateOrderWorkflowUseCase tests")
@ExtendWith(MockitoExtension.class)
class StartCreateOrderWorkflowUseCaseTest {

    private static final CreateOrderRequest REQUEST = createOrderRequest();
    private static final UUID ORDER_ID = UUID.randomUUID();

    @Mock
    private WorkflowRepository repository;

    @Mock
    private WorkflowMapper mapper;

    @Mock
    private OrderServiceClient orderServiceClient;

    @InjectMocks
    private StartCreateOrderWorkflowUseCase useCase;

    private Workflow wStarted;
    private Workflow wOrderCreated;
    private Workflow wOrderConfirmed;
    private Workflow wCompensating;
    private Workflow wCompleted;
    private Workflow wCompensated;
    private Workflow wFailed;

    private WorkflowEntity eStarted;
    private WorkflowEntity eOrderCreated;
    private WorkflowEntity eOrderConfirmed;
    private WorkflowEntity eCompensating;
    private WorkflowEntity eCompleted;
    private WorkflowEntity eCompensated;
    private WorkflowEntity eFailed;

    @BeforeEach
    void setUp() {
        wStarted = getWorkflow();
        wOrderCreated  = getWorkflow().toBuilder().state(WorkflowState.ORDER_CREATED).build();
        wOrderConfirmed = getWorkflow().toBuilder().state(WorkflowState.ORDER_CONFIRMED).build();
        wCompensating = getWorkflow().toBuilder().state(WorkflowState.COMPENSATING).build();
        wCompleted = getWorkflow().toBuilder().state(WorkflowState.COMPLETED).build();
        wCompensated = getWorkflow().toBuilder().state(WorkflowState.COMPENSATED).build();
        wFailed = getWorkflow().toBuilder().state(WorkflowState.FAILED).build();

        eStarted = getWorkflowEntity();
        eOrderCreated = getWorkflowEntity().toBuilder().state(WorkflowState.ORDER_CREATED).build();
        eOrderConfirmed = getWorkflowEntity().toBuilder().state(WorkflowState.ORDER_CONFIRMED).build();
        eCompensating = getWorkflowEntity().toBuilder().state(WorkflowState.COMPENSATING).build();
        eCompleted = getWorkflowEntity().toBuilder().state(WorkflowState.COMPLETED).build();
        eCompensated = getWorkflowEntity().toBuilder().state(WorkflowState.COMPENSATED).build();
        eFailed  = getWorkflowEntity().toBuilder().state(WorkflowState.FAILED).build();

        when(mapper.toEntity(any(Workflow.class))).thenAnswer(inv -> {
            Workflow w = inv.getArgument(0);
            return switch (w.state()) {
                case STARTED -> eStarted;
                case ORDER_CREATED -> eOrderCreated;
                case ORDER_CONFIRMED -> eOrderConfirmed;
                case COMPENSATING -> eCompensating;
                case COMPLETED -> eCompleted;
                case COMPENSATED -> eCompensated;
                case FAILED -> eFailed;
            };
        });

        when(repository.save(any(WorkflowEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        when(mapper.toDomain(any(WorkflowEntity.class))).thenAnswer(inv -> {
            WorkflowEntity e = inv.getArgument(0);
            return switch (e.getState()) {
                case STARTED -> wStarted;
                case ORDER_CREATED -> wOrderCreated;
                case ORDER_CONFIRMED -> wOrderConfirmed;
                case COMPENSATING -> wCompensating;
                case COMPLETED -> wCompleted;
                case COMPENSATED -> wCompensated;
                case FAILED -> wFailed;
            };
        });
    }

    @Test
    void execute_shouldCompleteWorkflow_whenHappyPath() {
        // Arrange
        when(orderServiceClient.createOrder(REQUEST)).thenReturn(ORDER_ID);

        // Act
        Workflow result = useCase.execute(REQUEST);

        // Assert
        assertThat(result.state()).isEqualTo(WorkflowState.COMPLETED);
        verify(orderServiceClient).createOrder(REQUEST);
        verify(orderServiceClient).confirmOrder(ORDER_ID);
    }

    @Test
    void execute_shouldFailWorkflow_whenOrderCreationFails() {
        // Arrange
        when(orderServiceClient.createOrder(REQUEST)).thenThrow(new RuntimeException("create failed"));

        // Act
        Workflow result = useCase.execute(REQUEST);

        // Assert
        assertThat(result.state()).isEqualTo(WorkflowState.FAILED);
        verify(orderServiceClient, never()).confirmOrder(ORDER_ID);
        verify(orderServiceClient, never()).cancelOrder(ORDER_ID);
    }

    @Test
    void execute_shouldCompensateWorkflow_whenConfirmFailsAndCancelSucceeds() {
        // Arrange
        when(orderServiceClient.createOrder(REQUEST)).thenReturn(ORDER_ID);
        doThrow(new RuntimeException("confirm failed")).when(orderServiceClient).confirmOrder(ORDER_ID);

        // Act
        Workflow result = useCase.execute(REQUEST);

        // Assert
        assertThat(result.state()).isEqualTo(WorkflowState.COMPENSATED);
        verify(orderServiceClient).cancelOrder(ORDER_ID);
    }

    @Test
    void execute_shouldFailWorkflow_whenConfirmFailsAndCancelAlsoFails() {
        // Arrange
        when(orderServiceClient.createOrder(REQUEST)).thenReturn(ORDER_ID);
        doThrow(new RuntimeException("confirm failed")).when(orderServiceClient).confirmOrder(ORDER_ID);
        doThrow(new RuntimeException("cancel failed")).when(orderServiceClient).cancelOrder(ORDER_ID);

        // Act
        Workflow result = useCase.execute(REQUEST);

        // Assert
        assertThat(result.state()).isEqualTo(WorkflowState.FAILED);
        verify(orderServiceClient).cancelOrder(ORDER_ID);
    }
}