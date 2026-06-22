package com.microservices.margo.workflow_service.core.infrastructure.client;

import static org.junit.jupiter.api.Assertions.*;

import com.microservices.margo.workflow_service.core.application.request.CreateOrderRequest;
import com.microservices.margo.workflow_service.core.infrastructure.config.OrderServiceProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class OrderServiceClientTest {

    @Mock RestClient restClient;

    private OrderServiceClient client;

    @BeforeEach
    void setUp() {
        OrderServiceProperties props = new OrderServiceProperties(
                new OrderServiceProperties.Url("http://order-service", "/api/orders", "/api/orders/{id}/status"),
                new OrderServiceProperties.Params("ownerUserId", "itemName", "quantity", "price", "newStatus", "id")
        );
        client = new OrderServiceClient(restClient, props);
    }

    // --- createOrderFallback ---

    @Test
    void createOrderFallback_throws503_onConnectFailure() {
        ResourceAccessException ex = new ResourceAccessException("refused");
        CreateOrderRequest req = new CreateOrderRequest(UUID.randomUUID(), "Latte", 1, BigDecimal.ONE);

        assertThatThrownBy(() -> client.createOrderFallback(ex, req))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void createOrderFallback_throws504_onSocketTimeout() {
        ResourceAccessException ex = new ResourceAccessException("timeout",
                new SocketTimeoutException("read timed out"));
        CreateOrderRequest req = new CreateOrderRequest(UUID.randomUUID(), "Latte", 1, BigDecimal.ONE);

        assertThatThrownBy(() -> client.createOrderFallback(ex, req))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
    }

    // --- confirmOrderFallback ---

    @Test
    void confirmOrderFallback_throws503_onConnectFailure() {
        ResourceAccessException ex = new ResourceAccessException("refused");
        UUID orderId = UUID.randomUUID();

        assertThatThrownBy(() -> client.confirmOrderFallback(ex, orderId))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void confirmOrderFallback_throws504_onSocketTimeout() {
        ResourceAccessException ex = new ResourceAccessException("timeout",
                new SocketTimeoutException("timed out"));

        assertThatThrownBy(() -> client.confirmOrderFallback(ex, UUID.randomUUID()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
    }

    // --- cancelOrderFallback ---

    @Test
    void cancelOrderFallback_throws503_onConnectFailure() {
        ResourceAccessException ex = new ResourceAccessException("refused");

        assertThatThrownBy(() -> client.cancelOrderFallback(ex, UUID.randomUUID()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void cancelOrderFallback_throws504_onSocketTimeout() {
        ResourceAccessException ex = new ResourceAccessException("timeout",
                new SocketTimeoutException("timed out"));

        assertThatThrownBy(() -> client.cancelOrderFallback(ex, UUID.randomUUID()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
    }
}