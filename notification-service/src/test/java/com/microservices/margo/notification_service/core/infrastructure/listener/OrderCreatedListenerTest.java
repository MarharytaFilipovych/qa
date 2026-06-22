package com.microservices.margo.notification_service.core.infrastructure.listener;

import com.microservices.margo.notification_service.core.application.event.OrderCreatedEvent;
import com.microservices.margo.notification_service.core.application.usecase.StoreNotificationUseCase;
import com.microservices.margo.notification_service.core.infrastructure.config.CorrelationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderCreatedListenerTest {

    @Mock StoreNotificationUseCase storeNotification;
    @Mock Jackson2JsonMessageConverter converter;
    @Mock CorrelationProperties correlationProperties;

    @InjectMocks OrderCreatedListener listener;

    private OrderCreatedEvent sampleEvent() {
        return new OrderCreatedEvent(UUID.randomUUID(), Instant.now(), "corr-abc",
                UUID.randomUUID(), UUID.randomUUID(), "payload");
    }

    @BeforeEach
    void setUp() {
        when(correlationProperties.header()).thenReturn("X-Correlation-Id");
        when(correlationProperties.key()).thenReturn("correlationId");
    }

    private Message buildMessage(String correlationId) {
        MessageProperties props = new MessageProperties();
        if (correlationId != null) {
            props.setHeader("X-Correlation-Id", correlationId);
        }
        return new Message(new byte[0], props);
    }

    @Test
    void onOrderCreated_delegatesToUseCase() {
        OrderCreatedEvent event = sampleEvent();
        Message message = buildMessage("corr-abc");
        when(converter.fromMessage(message)).thenReturn(event);

        listener.onOrderCreated(message);

        verify(storeNotification).execute(event);
    }

    @Test
    void onOrderCreated_usesUnknown_whenHeaderAbsent() {
        OrderCreatedEvent event = sampleEvent();
        Message message = buildMessage(null);
        when(converter.fromMessage(message)).thenReturn(event);

        listener.onOrderCreated(message);

        // MDC must be cleaned up after the call
        assertThat(MDC.get("correlationId")).isNull();
    }

    @Test
    void onOrderCreated_clearsMdc_afterProcessing() {
        Message message = buildMessage("corr-xyz");
        when(converter.fromMessage(message)).thenReturn(sampleEvent());

        listener.onOrderCreated(message);

        assertThat(MDC.get("correlationId")).isNull();
    }

    @Test
    void onOrderCreated_clearsMdc_evenWhenUseCaseThrows() {
        Message message = buildMessage("corr-xyz");
        when(converter.fromMessage(message)).thenReturn(sampleEvent());
        doThrow(new RuntimeException("fail")).when(storeNotification).execute(any());

        try {
            listener.onOrderCreated(message);
        } catch (RuntimeException ignored) {}

        assertThat(MDC.get("correlationId")).isNull();
    }
}