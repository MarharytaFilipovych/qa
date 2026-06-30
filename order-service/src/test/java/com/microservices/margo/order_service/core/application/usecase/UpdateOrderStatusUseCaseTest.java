package com.microservices.margo.order_service.core.application.usecase;

import com.microservices.margo.order_service.core.application.mapper.OrderMapper;
import com.microservices.margo.order_service.core.application.request.UpdateOrderStatusRequest;
import com.microservices.margo.order_service.core.domain.Order;
import com.microservices.margo.order_service.core.domain.OrderStatus;
import com.microservices.margo.order_service.core.infrastructure.entity.OrderEntity;
import com.microservices.margo.order_service.core.infrastructure.repository.OrderRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static com.microservices.margo.order_service.data.OrderData.getOrder;
import static com.microservices.margo.order_service.data.OrderData.getOrderEntity;
import static com.microservices.margo.order_service.data.OrderData.updateStatusRequest;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("UpdateOrderStatusUseCase tests")
@ExtendWith(MockitoExtension.class)
class UpdateOrderStatusUseCaseTest {

    private static final Order ORDER = getOrder();
    private static final OrderEntity ORDER_ENTITY = getOrderEntity();

    @Mock private OrderMapper orderMapper;
    @Mock private OrderRepository orderRepository;

    @InjectMocks
    private UpdateOrderStatusUseCase updateOrderStatusUseCase;

    @Test
    void execute_shouldUpdateOrderStatus() {
        // Arrange
        UpdateOrderStatusRequest request = updateStatusRequest(OrderStatus.CONFIRMED);
        Order confirmedOrder = ORDER.toBuilder().status(OrderStatus.CONFIRMED).build();
        OrderEntity confirmedEntity = ORDER_ENTITY.toBuilder().status(OrderStatus.CONFIRMED).build();

        when(orderRepository.findById(ORDER.id())).thenReturn(Optional.of(ORDER_ENTITY));
        when(orderMapper.toDomain(ORDER_ENTITY)).thenReturn(ORDER);
        when(orderMapper.toEntity(confirmedOrder)).thenReturn(confirmedEntity);

        // Act & Assert
        assertThatNoException().isThrownBy(() -> updateOrderStatusUseCase.execute(ORDER.id(), request));
        verify(orderRepository).save(confirmedEntity);
    }

    @Test
    void execute_shouldThrowWhenOrderNotFound() {
        // Arrange
        UUID orderId = ORDER.id();
        UpdateOrderStatusRequest request = updateStatusRequest(OrderStatus.CONFIRMED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> updateOrderStatusUseCase.execute(orderId, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Order not found: " + orderId);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void execute_shouldThrowWhenTransitionIsInvalid() {
        // Arrange
        UpdateOrderStatusRequest request = updateStatusRequest(OrderStatus.DELIVERED);
        when(orderRepository.findById(ORDER.id())).thenReturn(Optional.of(ORDER_ENTITY));
        when(orderMapper.toDomain(ORDER_ENTITY)).thenReturn(ORDER); // PENDING
        UUID orderId = ORDER.id();

        // Act & Assert
        assertThatThrownBy(() -> updateOrderStatusUseCase.execute(orderId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot change order status from PENDING to DELIVERED");
        verify(orderRepository, never()).save(any());
    }
}