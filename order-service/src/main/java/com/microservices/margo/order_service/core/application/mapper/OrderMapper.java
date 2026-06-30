package com.microservices.margo.order_service.core.application.mapper;

import com.microservices.margo.order_service.core.application.request.CreateOrderRequest;
import com.microservices.margo.order_service.core.domain.Order;
import com.microservices.margo.order_service.core.domain.OrderStatus;
import com.microservices.margo.order_service.core.infrastructure.entity.OrderEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", imports = OrderStatus.class)
public interface OrderMapper {
    OrderEntity toEntity(Order order);

    Order toDomain(OrderEntity entity);

    @Mapping(target = "status", expression = "java(OrderStatus.PENDING)")
    OrderEntity toEntity(CreateOrderRequest createOrderRequest);
}
