package com.microservices.margo.order_service.core.infrastructure.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitMQConfigTest {

    private RabbitMQConfig config;
    private final RabbitMQProperties props =
            new RabbitMQProperties("order", "order.created", "order.created");

    @BeforeEach
    void setUp() {
        config = new RabbitMQConfig(props);
    }

    @Test
    void coreExchange_hasCorrectName() {
        TopicExchange exchange = config.coreExchange();
        assertThat(exchange.getName()).isEqualTo("order");
    }

    @Test
    void orderCreatedQueue_isDurableWithCorrectName() {
        Queue queue = config.orderCreatedQueue();
        assertThat(queue.getName()).isEqualTo("order.created");
        assertThat(queue.isDurable()).isTrue();
    }

    @Test
    void orderCreatedBinding_bindsQueueToExchangeWithRoutingKey() {
        Queue queue = config.orderCreatedQueue();
        TopicExchange exchange = config.coreExchange();
        Binding binding = config.orderCreatedBinding(queue, exchange);

        assertThat(binding.getDestination()).isEqualTo("order.created");
        assertThat(binding.getExchange()).isEqualTo("order");
        assertThat(binding.getRoutingKey()).isEqualTo("order.created");
    }

    @Test
    void messageConverter_isNotNull() {
        Jackson2JsonMessageConverter converter = config.messageConverter();
        assertThat(converter).isNotNull();
    }
}