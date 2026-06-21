package com.microservices.margo.notification_service.core.application.mapper;

import com.microservices.margo.notification_service.core.infrastructure.entity.NotificationEntity;
import com.microservices.margo.notification_service.core.application.event.OrderCreatedEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    @Mapping(target = "coreItemId", source = "orderId")
    @Mapping(target = "createdAt", source = "occurredAt")
    NotificationEntity toEntity(OrderCreatedEvent event);
}
