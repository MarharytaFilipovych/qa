package com.microservices.margo.order_service.core.application.usecase;

import com.microservices.margo.order_service.core.application.mapper.OrderMapper;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetOrderUseCaseTest {

    @Mock OrderMapper orderMapper;
    @Mock OrderRepository orderRepository;

    @InjectMocks GetOrderUseCase useCase;

    @Test
    void execute_returnsOrder_whenFound() {
        UUID id = UUID.randomUUID();
        OrderEntity entity = new OrderEntity();
        Order order = Order.builder().id(id).itemName("Latte").quantity(1)
                .price(BigDecimal.ONE).ownerUserId(UUID.randomUUID())
                .status(OrderStatus.PENDING).build();

        when(orderRepository.findById(id)).thenReturn(Optional.of(entity));
        when(orderMapper.toDomain(entity)).thenReturn(order);

        assertThat(useCase.execute(id)).isEqualTo(order);
    }

    @Test
    void execute_throwsEntityNotFound_whenMissing() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(id))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(id.toString());
    }
}