package com.microservices.margo.order_service.core.application.mapper;

import com.microservices.margo.order_service.core.application.request.CreateOrderRequest;
import com.microservices.margo.order_service.core.domain.Order;
import com.microservices.margo.order_service.core.domain.OrderStatus;
import com.microservices.margo.order_service.core.infrastructure.entity.OrderEntity;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", imports = OrderStatus.class)
public interface OrderMapper {
    OrderEntity toEntity(Order order);

    Order toDomain(OrderEntity entity);

    @Mapping(target = "status", expression = "java(OrderStatus.PENDING)")
    OrderEntity toEntity(CreateOrderRequest createOrderRequest);

    @AfterMapping
    default void replayStatus(@MappingTarget Order order, OrderEntity entity) {
        var current = order.status();
        if (current == entity.getStatus()) return;

        switch (entity.getStatus()) {
            case CONFIRMED -> order.changeStatus(OrderStatus.CONFIRMED);
            case DELIVERED -> {
                if (current == OrderStatus.PENDING)
                    order.changeStatus(OrderStatus.CONFIRMED);
                order.changeStatus(OrderStatus.DELIVERED);
            }
            case CANCELLED -> order.changeStatus(OrderStatus.CANCELLED);
        }
    }
}
