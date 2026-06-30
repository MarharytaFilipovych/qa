package com.microservices.margo.order_service.core.application.usecase;

import com.microservices.margo.order_service.core.application.mapper.OrderMapper;
import com.microservices.margo.order_service.core.domain.Order;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("GetOrderUseCase tests")
@ExtendWith(MockitoExtension.class)
class GetOrderUseCaseTest {

    private static final OrderEntity ORDER_ENTITY = getOrderEntity();
    private static final Order ORDER = getOrder();

    @Mock private OrderMapper orderMapper;
    @Mock private OrderRepository orderRepository;

    @InjectMocks
    private GetOrderUseCase getOrderUseCase;

    @Test
    void execute_shouldFindOrderByIdAndReturnIt() {
        // Arrange
        when(orderRepository.findById(ORDER.id())).thenReturn(Optional.of(ORDER_ENTITY));
        when(orderMapper.toDomain(ORDER_ENTITY)).thenReturn(ORDER);

        // Act
        Order result = getOrderUseCase.execute(ORDER.id());

        // Assert
        assertThat(result).isEqualTo(ORDER);
        verify(orderRepository).findById(ORDER.id());
        verify(orderMapper).toDomain(ORDER_ENTITY);
    }

    @Test
    void execute_shouldThrowWhenOrderIsNotFound() {
        // Arrange
        UUID orderId = ORDER.id();
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> getOrderUseCase.execute(orderId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Order with id: " + orderId + " was not found!");
        verify(orderMapper, never()).toDomain(ORDER_ENTITY);
    }
}