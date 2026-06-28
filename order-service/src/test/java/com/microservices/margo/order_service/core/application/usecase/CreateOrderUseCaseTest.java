package com.microservices.margo.order_service.core.application.usecase;

import com.microservices.margo.order_service.core.application.mapper.OrderMapper;
import com.microservices.margo.order_service.core.application.request.CreateOrderRequest;
import com.microservices.margo.order_service.core.domain.Order;
import com.microservices.margo.order_service.core.infrastructure.client.UserValidationClient;
import com.microservices.margo.order_service.core.infrastructure.entity.OrderEntity;
import com.microservices.margo.order_service.core.infrastructure.publisher.OrderEventPublisher;
import com.microservices.margo.order_service.core.infrastructure.repository.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.microservices.margo.order_service.data.OrderData.createOrderRequest;
import static com.microservices.margo.order_service.data.OrderData.getOrder;
import static com.microservices.margo.order_service.data.OrderData.getOrderEntity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("CreateOrderUseCase tests")
@ExtendWith(MockitoExtension.class)
class CreateOrderUseCaseTest {

    private static final CreateOrderRequest CREATE_ORDER_REQUEST = createOrderRequest();
    private static final OrderEntity ORDER_ENTITY = getOrderEntity();
    private static final Order ORDER = getOrder();

    @Mock private OrderMapper orderMapper;
    @Mock private OrderRepository orderRepository;
    @Mock private UserValidationClient userValidationClient;
    @Mock private OrderEventPublisher eventPublisher;

    @InjectMocks
    private CreateOrderUseCase createOrderUseCase;

    @Test
    void execute_shouldValidateUserSaveAndPublishEvent() {
        // Arrange
        when(orderMapper.toEntity(CREATE_ORDER_REQUEST)).thenReturn(ORDER_ENTITY);
        when(orderRepository.save(ORDER_ENTITY)).thenReturn(ORDER_ENTITY);
        when(orderMapper.toDomain(ORDER_ENTITY)).thenReturn(ORDER);

        // Act
        Order result = createOrderUseCase.execute(CREATE_ORDER_REQUEST);

        // Assert
        assertThat(result).isEqualTo(ORDER);
        verify(userValidationClient).validateUserExists(CREATE_ORDER_REQUEST.ownerUserId());
        verify(orderRepository).save(ORDER_ENTITY);
        verify(eventPublisher).publishOrderCreated(ORDER);
    }

    @Test
    void execute_shouldPropagateExceptionWhenUserValidationFails() {
        // Arrange
        RuntimeException cause = new RuntimeException("User not found");
        doThrow(cause).when(userValidationClient).validateUserExists(CREATE_ORDER_REQUEST.ownerUserId());

        // Act & Assert
        assertThatThrownBy(() -> createOrderUseCase.execute(CREATE_ORDER_REQUEST))
                .isEqualTo(cause);
        verify(orderRepository, never()).save(ORDER_ENTITY);
        verify(eventPublisher, never()).publishOrderCreated(ORDER);
    }
}