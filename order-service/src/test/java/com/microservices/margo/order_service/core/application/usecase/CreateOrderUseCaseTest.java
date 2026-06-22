package com.microservices.margo.order_service.core.application.usecase;

import com.microservices.margo.order_service.core.application.mapper.OrderMapper;
import com.microservices.margo.order_service.core.application.request.CreateOrderRequest;
import com.microservices.margo.order_service.core.domain.Order;
import com.microservices.margo.order_service.core.domain.OrderStatus;
import com.microservices.margo.order_service.core.infrastructure.client.UserValidationClient;
import com.microservices.margo.order_service.core.infrastructure.entity.OrderEntity;
import com.microservices.margo.order_service.core.infrastructure.publisher.OrderEventPublisher;
import com.microservices.margo.order_service.core.infrastructure.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateOrderUseCaseTest {

    @Mock OrderMapper orderMapper;
    @Mock OrderRepository orderRepository;
    @Mock UserValidationClient userValidationClient;
    @Mock OrderEventPublisher eventPublisher;

    @InjectMocks CreateOrderUseCase useCase;

    private CreateOrderRequest request;
    private OrderEntity savedEntity;
    private Order mappedOrder;

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();
        request = new CreateOrderRequest("Latte", 2, BigDecimal.valueOf(5.99), userId);

        savedEntity = new OrderEntity();
        savedEntity.setId(UUID.randomUUID());
        savedEntity.setStatus(OrderStatus.PENDING);

        mappedOrder = Order.builder()
                .id(savedEntity.getId())
                .itemName("Latte")
                .quantity(2)
                .price(BigDecimal.valueOf(5.99))
                .ownerUserId(userId)
                .status(OrderStatus.PENDING)
                .build();
    }

    @Test
    void execute_validatesUserThenSavesAndPublishes() {
        when(orderMapper.toEntity(request)).thenReturn(savedEntity);
        when(orderRepository.save(savedEntity)).thenReturn(savedEntity);
        when(orderMapper.toDomain(savedEntity)).thenReturn(mappedOrder);

        Order result = useCase.execute(request);

        verify(userValidationClient).validateUserExists(request.ownerUserId());
        verify(orderRepository).save(savedEntity);
        verify(eventPublisher).publishOrderCreated(mappedOrder);
        assertThat(result).isEqualTo(mappedOrder);
    }

    @Test
    void execute_whenUserValidationFails_throwsAndDoesNotSave() {
        doThrow(new RuntimeException("user not found"))
                .when(userValidationClient).validateUserExists(any());

        assertThatThrownBy(() -> useCase.execute(request))
                .hasMessage("user not found");

        verifyNoInteractions(orderRepository, eventPublisher);
    }

    @Test
    void execute_publishesEventAfterSave() {
        when(orderMapper.toEntity(request)).thenReturn(savedEntity);
        when(orderRepository.save(savedEntity)).thenReturn(savedEntity);
        when(orderMapper.toDomain(savedEntity)).thenReturn(mappedOrder);

        useCase.execute(request);

        // event must be published with the domain object returned by the mapper
        verify(eventPublisher).publishOrderCreated(mappedOrder);
    }
}