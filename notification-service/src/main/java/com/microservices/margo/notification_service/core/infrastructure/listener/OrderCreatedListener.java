package com.microservices.margo.notification_service.core.infrastructure.listener;

import com.microservices.margo.notification_service.core.application.event.OrderCreatedEvent;
import com.microservices.margo.notification_service.core.application.usecase.StoreNotificationUseCase;
import com.microservices.margo.notification_service.core.infrastructure.config.CorrelationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreatedListener {
    private final CorrelationProperties correlationProperties;
    private final StoreNotificationUseCase storeNotification;
    private final Jackson2JsonMessageConverter converter;

    @RabbitListener(queues = "${rabbitmq.queue}")
    public void onOrderCreated(Message message) {
        String correlationId = (String) message.getMessageProperties()
                .getHeaders().getOrDefault(correlationProperties.header(), "unknown");
        MDC.put(correlationProperties.key(), correlationId);
        try {
            OrderCreatedEvent event = (OrderCreatedEvent) converter.fromMessage(message);
            log.info("Received OrderCreatedEvent {}", event);
            storeNotification.execute(event);
        } finally {
            MDC.remove(correlationProperties.key());
        }
    }
}