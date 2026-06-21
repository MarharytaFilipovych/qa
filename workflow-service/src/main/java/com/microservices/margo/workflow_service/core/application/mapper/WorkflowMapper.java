package com.microservices.margo.workflow_service.core.application.mapper;

import com.microservices.margo.workflow_service.core.domain.Workflow;
import com.microservices.margo.workflow_service.core.infrastructure.entity.WorkflowEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface WorkflowMapper {
    WorkflowEntity toEntity(Workflow domain);
    Workflow toDomain(WorkflowEntity entity);
}