package com.microservices.margo.order_service.core.application.usecase;

import com.microservices.margo.order_service.core.application.mapper.OrderMapper;
import com.microservices.margo.order_service.core.application.request.UpdateOrderStatusRequest;
import com.microservices.margo.order_service.core.domain.Order;
import com.microservices.margo.order_service.core.domain.OrderStatus;
import com.microservices.margo.order_service.core.infrastructure.entity.OrderEntity;
import com.microservices.margo.order_service.core.infrastructure.repository.OrderRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateOrderStatusUseCaseTest {

    @Mock OrderMapper orderMapper;
    @Mock OrderRepository orderRepository;

    @InjectMocks UpdateOrderStatusUseCase useCase;

    private Order pendingOrder(UUID id) {
        return Order.builder().id(id).itemName("Latte").quantity(1)
                .price(BigDecimal.ONE).ownerUserId(UUID.randomUUID())
                .status(OrderStatus.PENDING).build();
    }

    @Test
    void execute_savesUpdatedOrder() {
        UUID id = UUID.randomUUID();
        Order order = pendingOrder(id);
        OrderEntity entity = new OrderEntity();
        OrderEntity updatedEntity = new OrderEntity();

        when(orderRepository.findById(id)).thenReturn(Optional.of(entity));
        when(orderMapper.toDomain(entity)).thenReturn(order);
        when(orderMapper.toEntity(any(Order.class))).thenReturn(updatedEntity);

        useCase.execute(id, new UpdateOrderStatusRequest(OrderStatus.CONFIRMED));

        verify(orderRepository).save(updatedEntity);
    }

    @Test
    void execute_throwsEntityNotFound_whenMissing() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(id, new UpdateOrderStatusRequest(OrderStatus.CONFIRMED)))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(id.toString());

        verify(orderRepository, never()).save(any());
    }

    @Test
    void execute_throwsIllegalState_forInvalidTransition() {
        UUID id = UUID.randomUUID();
        Order order = pendingOrder(id);
        OrderEntity entity = new OrderEntity();

        when(orderRepository.findById(id)).thenReturn(Optional.of(entity));
        when(orderMapper.toDomain(entity)).thenReturn(order);

        // PENDING -> DELIVERED is illegal
        assertThatThrownBy(() -> useCase.execute(id, new UpdateOrderStatusRequest(OrderStatus.DELIVERED)))
                .isInstanceOf(IllegalStateException.class);

        verify(orderRepository, never()).save(any());
    }
}