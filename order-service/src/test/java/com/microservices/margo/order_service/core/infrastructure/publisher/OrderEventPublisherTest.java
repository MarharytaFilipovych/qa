package com.microservices.margo.order_service.core.infrastructure.publisher;

import com.microservices.margo.order_service.core.domain.Order;
import com.microservices.margo.order_service.core.domain.OrderStatus;
import com.microservices.margo.order_service.core.infrastructure.config.CorrelationProperties;
import com.microservices.margo.order_service.core.infrastructure.config.RabbitMQProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderEventPublisherTest {

    @Mock RabbitTemplate rabbitTemplate;
    @Mock CorrelationProperties correlationProperties;
    @Mock RabbitMQProperties rabbitMQProperties;

    @InjectMocks OrderEventPublisher publisher;

    private Order order;

    @BeforeEach
    void setUp() {
        order = Order.builder()
                .id(UUID.randomUUID())
                .itemName("Latte")
                .quantity(2)
                .price(BigDecimal.valueOf(5.99))
                .ownerUserId(UUID.randomUUID())
                .status(OrderStatus.PENDING)
                .build();

        when(correlationProperties.key()).thenReturn("correlationId");
        when(correlationProperties.header()).thenReturn("X-Correlation-Id");
        when(rabbitMQProperties.exchange()).thenReturn("order");
        when(rabbitMQProperties.routingKey()).thenReturn("order.created");
    }

    @AfterEach
    void tearDown() { MDC.clear(); }

    @Test
    void publishOrderCreated_sendsToCorrectExchangeAndRoutingKey() {
        publisher.publishOrderCreated(order);

        verify(rabbitTemplate).convertAndSend(
                eq("order"),
                eq("order.created"),
                any(),
                any(MessagePostProcessor.class)
        );
    }

    @Test
    void publishOrderCreated_usesCorrelationIdFromMdc() {
        MDC.put("correlationId", "mdc-corr-id");

        publisher.publishOrderCreated(order);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), eventCaptor.capture(), any(MessagePostProcessor.class));

        // The event is an OrderCreatedEvent — check correlation id was read from MDC
        var event = eventCaptor.getValue();
        assertThat(event.toString()).contains("mdc-corr-id");
    }

    @Test
    void publishOrderCreated_generatesCorrelationId_whenMdcEmpty() {
        // No MDC entry set — should still publish without throwing
        publisher.publishOrderCreated(order);

        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(), any(MessagePostProcessor.class));
    }
}