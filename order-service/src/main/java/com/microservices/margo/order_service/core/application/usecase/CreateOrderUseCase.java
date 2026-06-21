package com.microservices.margo.order_service.core.application.usecase;

import com.microservices.margo.order_service.core.application.request.CreateOrderRequest;
import com.microservices.margo.order_service.core.application.mapper.OrderMapper;
import com.microservices.margo.order_service.core.domain.Order;
import com.microservices.margo.order_service.core.infrastructure.client.UserValidationClient;
import com.microservices.margo.order_service.core.infrastructure.entity.OrderEntity;
import com.microservices.margo.order_service.core.infrastructure.publisher.OrderEventPublisher;
import com.microservices.margo.order_service.core.infrastructure.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateOrderUseCase {
    private final OrderMapper orderMapper;
    private final OrderRepository orderRepository;
    private final UserValidationClient userValidationClient;
    private final OrderEventPublisher eventPublisher;

    public Order execute(CreateOrderRequest command) {
        log.info("Validating user {} before placing order", command.ownerUserId());
        userValidationClient.validateUserExists(command.ownerUserId());

        log.info("Placing order for customer='{}', item='{}'",
                command.ownerUserId(), command.itemName());
        OrderEntity entity = orderRepository.save(orderMapper.toEntity(command));
        Order order = orderMapper.toDomain(entity);
        eventPublisher.publishOrderCreated(order);
        return order;
    }
}
