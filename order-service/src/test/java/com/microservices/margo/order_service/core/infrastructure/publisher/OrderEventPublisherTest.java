package com.microservices.margo.order_service.core.infrastructure.publisher;

import com.microservices.margo.order_service.core.infrastructure.config.CorrelationProperties;
import com.microservices.margo.order_service.core.infrastructure.config.RabbitMQProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static com.microservices.margo.order_service.data.Constants.CORRELATION_ID;
import static com.microservices.margo.order_service.data.Constants.CORRELATION_ID_HEADER;
import static com.microservices.margo.order_service.data.Constants.MDC_KEY;
import static com.microservices.margo.order_service.data.OrderData.getOrder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("OrderEventPublisher tests")
class OrderEventPublisherTest {

    private static final String EXCHANGE = "order";
    private static final String ROUTING_KEY = "order.created";

    @Mock
    private CorrelationProperties correlationProperties;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private RabbitMQProperties rabbitMQProperties;

    @InjectMocks
    private OrderEventPublisher publisher;

    @BeforeEach
    void setUp() {
        when(correlationProperties.key()).thenReturn(MDC_KEY);
        when(correlationProperties.header()).thenReturn(CORRELATION_ID_HEADER);
        when(rabbitMQProperties.exchange()).thenReturn(EXCHANGE);
        when(rabbitMQProperties.routingKey()).thenReturn(ROUTING_KEY);
    }

    @AfterEach
    void clearMdc() {
        MDC.remove(MDC_KEY);
    }

    @Test
    void publishOrderCreated_shouldSendEventWithCorrelationIdFromMdc() {
        // Arrange
        MDC.put(MDC_KEY, CORRELATION_ID);

        // Act
        publisher.publishOrderCreated(getOrder());

        // Assert
        verify(rabbitTemplate).convertAndSend(eq(EXCHANGE), eq(ROUTING_KEY), any(), any(MessagePostProcessor.class));
    }

    @Test
    void publishOrderCreated_shouldGenerateCorrelationIdWhenAbsentFromMdc() {
        // Act
        publisher.publishOrderCreated(getOrder());

        // Assert
        verify(rabbitTemplate).convertAndSend(eq(EXCHANGE), eq(ROUTING_KEY), any(), any(MessagePostProcessor.class));
    }

    @Test
    void publishOrderCreated_messagePostProcessor_shouldSetCorrelationHeader() {
        // Arrange
        MDC.put(MDC_KEY, CORRELATION_ID);

        ArgumentCaptor<MessagePostProcessor> processorCaptor =
                ArgumentCaptor.forClass(MessagePostProcessor.class);

        // Act
        publisher.publishOrderCreated(getOrder());
        verify(rabbitTemplate).convertAndSend(any(), any(), any(), processorCaptor.capture());

        // Assert
        Message message = mock(Message.class);
        MessageProperties props = new MessageProperties();
        when(message.getMessageProperties()).thenReturn(props);

        processorCaptor.getValue().postProcessMessage(message);

        assertThat(props.getHeader(CORRELATION_ID_HEADER).toString()).hasToString(CORRELATION_ID);
    }
}