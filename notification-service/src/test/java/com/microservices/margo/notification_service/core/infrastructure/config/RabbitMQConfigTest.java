package com.microservices.margo.notification_service.core.infrastructure.config;

import com.microservices.margo.notification_service.core.application.event.OrderCreatedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.ClassMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class RabbitMQConfigTest {

    private final RabbitMQProperties properties = new RabbitMQProperties("order", "order.created", "order.created");
    private final RabbitMQConfig config = new RabbitMQConfig(properties);
    private static final String CLASS_MAPPER_FIELD = "classMapper";

    @Test
    void messageConverter_toClass_shouldAlwaysReturnOrderCreatedEvent() {
        // Arrange
        Jackson2JsonMessageConverter converter = config.messageConverter();
        ClassMapper classMapper = (ClassMapper) ReflectionTestUtils.getField(converter, CLASS_MAPPER_FIELD);
        assertThat(classMapper).isNotNull();

        // Act
        Class<?> result = classMapper.toClass(new MessageProperties());

        // Assert
        assertThat(result).isEqualTo(OrderCreatedEvent.class);
    }

    @Test
    void messageConverter_fromClass_shouldNotThrow() {
        // Arrange
        Jackson2JsonMessageConverter converter = config.messageConverter();
        ClassMapper classMapper = (ClassMapper) ReflectionTestUtils.getField(converter, CLASS_MAPPER_FIELD);
        assertThat(classMapper).isNotNull();

        // Act & Assert
        assertThatNoException().isThrownBy(
                () -> classMapper.fromClass(OrderCreatedEvent.class, new MessageProperties())
        );
    }
}