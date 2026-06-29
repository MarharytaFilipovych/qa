package com.microservices.margo.order_service.contract;

import com.microservices.margo.order_service.core.infrastructure.publisher.OrderEventPublisher;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestRabbitConfig {

    @Bean
    @Primary
    public OrderEventPublisher orderEventPublisher() {
        return Mockito.mock(OrderEventPublisher.class);
    }
}