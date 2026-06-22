package com.microservices.margo.workflow_service.core.application.usecase;

import com.microservices.margo.workflow_service.core.application.mapper.WorkflowMapper;
import com.microservices.margo.workflow_service.core.domain.Workflow;
import com.microservices.margo.workflow_service.core.domain.WorkflowState;
import com.microservices.margo.workflow_service.core.infrastructure.entity.WorkflowEntity;
import com.microservices.margo.workflow_service.core.infrastructure.repository.WorkflowRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetWorkflowUseCaseTest {

    @Mock WorkflowRepository repository;
    @Mock WorkflowMapper mapper;

    @InjectMocks GetWorkflowUseCase useCase;

    @Test
    void execute_returnsWorkflow_whenFound() {
        UUID id = UUID.randomUUID();
        WorkflowEntity entity = new WorkflowEntity();
        Workflow workflow = Workflow.builder().id(id).state(WorkflowState.COMPLETED).build();

        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(workflow);

        assertThat(useCase.execute(id)).isEqualTo(workflow);
    }

    @Test
    void execute_throwsEntityNotFound_whenMissing() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(id))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(id.toString());
    }
}