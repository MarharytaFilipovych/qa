package com.microservices.margo.order_service.core.application.usecase;

import com.microservices.margo.order_service.core.application.request.UpdateOrderStatusRequest;
import com.microservices.margo.order_service.core.application.mapper.OrderMapper;
import com.microservices.margo.order_service.core.domain.Order;
import com.microservices.margo.order_service.core.infrastructure.repository.OrderRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateOrderStatusUseCase {
    private final OrderMapper orderMapper;
    private final OrderRepository orderRepository;

    public void execute(UUID id, UpdateOrderStatusRequest command) {
        Order order = orderRepository.findById(id).map(orderMapper::toDomain)
            .orElseThrow(() -> new EntityNotFoundException("Order not found: " + id));

        log.info("Updating order {} status from {} to {}", id, order.status(), command.newStatus());
        Order updatedOrder = order.changeStatus(command.newStatus());
        orderRepository.save(orderMapper.toEntity(updatedOrder));
    }
}
