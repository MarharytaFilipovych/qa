package com.microservices.margo.workflow_service.core.application.usecase;

import com.microservices.margo.workflow_service.core.application.mapper.WorkflowMapper;
import com.microservices.margo.workflow_service.core.domain.Workflow;
import com.microservices.margo.workflow_service.core.infrastructure.entity.WorkflowEntity;
import com.microservices.margo.workflow_service.core.infrastructure.repository.WorkflowRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static com.microservices.margo.workflow_service.data.WorkflowData.getWorkflow;
import static com.microservices.margo.workflow_service.data.WorkflowData.getWorkflowEntity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("GetWorkflowUseCase tests")
@ExtendWith(MockitoExtension.class)
class GetWorkflowUseCaseTest {

    private static final Workflow WORKFLOW = getWorkflow();
    private static final WorkflowEntity WORKFLOW_ENTITY = getWorkflowEntity();

    @Mock
    private WorkflowRepository repository;

    @Mock
    private WorkflowMapper mapper;

    @InjectMocks
    private GetWorkflowUseCase getWorkflowUseCase;

    @Test
    void execute_shouldFindWorkflowByIdAndReturnIt() {
        // Arrange
        when(repository.findById(WORKFLOW.id())).thenReturn(Optional.of(WORKFLOW_ENTITY));
        when(mapper.toDomain(WORKFLOW_ENTITY)).thenReturn(WORKFLOW);

        // Act
        Workflow result = getWorkflowUseCase.execute(WORKFLOW.id());

        // Assert
        assertThat(result).isEqualTo(WORKFLOW);
        verify(repository).findById(WORKFLOW.id());
        verify(mapper).toDomain(WORKFLOW_ENTITY);
    }

    @Test
    void execute_shouldThrowWhenWorkflowIsNotFound() {
        // Arrange
        UUID workflowId = WORKFLOW.id();
        when(repository.findById(workflowId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> getWorkflowUseCase.execute(workflowId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Workflow not found: " + workflowId);
        verify(mapper, never()).toDomain(WORKFLOW_ENTITY);
    }
}