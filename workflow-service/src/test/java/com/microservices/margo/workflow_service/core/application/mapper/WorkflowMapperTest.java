package com.microservices.margo.workflow_service.core.application.mapper;

import com.microservices.margo.workflow_service.core.domain.Workflow;
import com.microservices.margo.workflow_service.core.infrastructure.entity.WorkflowEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static com.microservices.margo.workflow_service.data.WorkflowData.getWorkflow;
import static com.microservices.margo.workflow_service.data.WorkflowData.getWorkflowEntity;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WorkflowMapper tests")
class WorkflowMapperTest {
    private static final WorkflowEntity WORKFLOW_ENTITY = getWorkflowEntity();
    private static final Workflow WORKFLOW = getWorkflow();
    private final WorkflowMapper mapper = Mappers.getMapper(WorkflowMapper.class);

    @Test
    void toEntity_shouldMapAllFields() {
        // Act
        WorkflowEntity entity = mapper.toEntity(WORKFLOW);

        // Assert
        assertThat(entity).isEqualTo(WORKFLOW_ENTITY);
    }

    @Test
    void toEntity_ifWorkflowIsNull_shouldReturnNull() {
        // Act & Assert
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    void toDomain_shouldMapAllFields() {
        // Act
        Workflow result = mapper.toDomain(WORKFLOW_ENTITY);

        // Assert
        assertThat(result).isEqualTo(WORKFLOW);
    }

    @Test
    void toDomain_ifEntityIsNull_shouldReturnNull() {
        // Act & Assert
        assertThat(mapper.toDomain(null)).isNull();
    }
}