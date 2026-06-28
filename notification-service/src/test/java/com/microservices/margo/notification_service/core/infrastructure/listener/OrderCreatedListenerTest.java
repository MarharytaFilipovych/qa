package com.microservices.margo.notification_service.core.infrastructure.listener;

import com.microservices.margo.notification_service.core.application.event.OrderCreatedEvent;
import com.microservices.margo.notification_service.core.application.usecase.StoreNotificationUseCase;
import com.microservices.margo.notification_service.core.infrastructure.config.CorrelationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

import java.util.UUID;

import static com.microservices.margo.notification_service.data.NotificationData.getOrderCreatedEvent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("OrderCreatedListener tests")
@ExtendWith(MockitoExtension.class)
class OrderCreatedListenerTest {

    private static final String CORRELATION_HEADER = "X-Correlation-Id";
    private static final String MDC_KEY = "correlationId";
    private static final String CORRELATION_ID = UUID.randomUUID().toString();
    private static final OrderCreatedEvent EVENT = getOrderCreatedEvent();

    @Mock
    private CorrelationProperties correlationProperties;

    @Mock
    private StoreNotificationUseCase storeNotification;

    @Mock
    private Jackson2JsonMessageConverter converter;

    @InjectMocks
    private OrderCreatedListener listener;

    @BeforeEach
    void setUp() {
        when(correlationProperties.header()).thenReturn(CORRELATION_HEADER);
        when(correlationProperties.key()).thenReturn(MDC_KEY);
    }

    @Test
    void onOrderCreated_shouldReadCorrelationIdAndStoreNotification() {
        // Arrange
        MessageProperties props = new MessageProperties();
        props.setHeader(CORRELATION_HEADER, CORRELATION_ID);
        Message message = new Message(new byte[0], props);

        when(converter.fromMessage(message)).thenReturn(EVENT);

        try (MockedStatic<MDC> mdcMock = mockStatic(MDC.class)) {
            // Act
            listener.onOrderCreated(message);

            // Assert
            mdcMock.verify(() -> MDC.put(MDC_KEY, CORRELATION_ID));
            mdcMock.verify(() -> MDC.remove(MDC_KEY));
        }
        verify(storeNotification).execute(EVENT);
    }

    @Test
    @DisplayName("onOrderCreated should fall back to 'unknown' when correlation header is absent")
    void onOrderCreated_shouldFallBackToUnknownWhenCorrelationHeaderAbsent() {
        // Arrange
        MessageProperties props = new MessageProperties();
        Message message = new Message(new byte[0], props);

        when(converter.fromMessage(message)).thenReturn(EVENT);

        try (MockedStatic<MDC> mdcMock = mockStatic(MDC.class)) {
            // Act
            listener.onOrderCreated(message);

            // Assert
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            mdcMock.verify(() -> MDC.put(org.mockito.ArgumentMatchers.eq(MDC_KEY), captor.capture()));
            assertThat(captor.getValue()).isEqualTo("unknown");
            mdcMock.verify(() -> MDC.remove(MDC_KEY));
        }
    }

    @Test
    void onOrderCreated_shouldRemoveMdcEvenWhenExceptionOccurs() {
        // Arrange
        MessageProperties props = new MessageProperties();
        props.setHeader(CORRELATION_HEADER, CORRELATION_ID);
        Message message = new Message(new byte[0], props);

        when(converter.fromMessage(message)).thenThrow(new RuntimeException("parse error"));

        try (MockedStatic<MDC> mdcMock = mockStatic(MDC.class)) {
            // Act
            try {
                listener.onOrderCreated(message);
            } catch (RuntimeException ignored) {
                // ignored
            }

            // Assert
            mdcMock.verify(() -> MDC.remove(MDC_KEY));
        }
    }
}