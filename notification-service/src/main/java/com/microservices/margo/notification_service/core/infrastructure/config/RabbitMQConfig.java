package com.microservices.margo.notification_service.core.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.microservices.margo.notification_service.core.application.event.OrderCreatedEvent;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.ClassMapper;

@Configuration
@RequiredArgsConstructor
public class RabbitMQConfig {
    private final RabbitMQProperties properties;

    @Bean
    public TopicExchange coreExchange() {
        return new TopicExchange(properties.exchange());
    }

    @Bean
    public Queue orderCreatedQueue() {
        return QueueBuilder.durable(properties.queue()).build();
    }

    @Bean
    public Binding orderCreatedBinding(Queue orderCreatedQueue, TopicExchange coreExchange) {
        return BindingBuilder.bind(orderCreatedQueue)
                .to(coreExchange)
                .with(properties.routingKey());
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        ObjectMapper mapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(mapper);
        converter.setClassMapper(new ClassMapper() {
            @Override
            public void fromClass(Class<?> clazz, MessageProperties properties) {}

            @Override
            public Class<?> toClass(MessageProperties properties) {
                return OrderCreatedEvent.class;
            }
        });
        return converter;
    }
}