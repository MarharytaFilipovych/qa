package com.microservices.margo.workflow_service.core.infrastructure.client;

import com.microservices.margo.workflow_service.core.application.request.CreateOrderRequest;
import com.microservices.margo.workflow_service.core.domain.OrderStatus;
import com.microservices.margo.workflow_service.core.infrastructure.config.OrderServiceProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class OrderServiceClient {

    private final RestClient restClient;
    private final OrderServiceProperties.Url url;
    private final OrderServiceProperties.Params params;

    public OrderServiceClient(RestClient restClient, OrderServiceProperties orderServiceProperties) {
        this.restClient = restClient;
        this.url = orderServiceProperties.getUrl();
        this.params = orderServiceProperties.getParams();
    }

    @Retryable(retryFor = ResourceAccessException.class,
            maxAttemptsExpression = "${retry.maxAttempts}",
            backoff = @Backoff(delayExpression = "${retry.backoff-delay}"))
    public UUID createOrder(CreateOrderRequest request) {
        Map<?, ?> response = restClient.post()
                .uri(url.base() + url.createOrder())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        params.ownerUserId(), request.ownerUserId(),
                        params.itemName(), request.itemName(),
                        params.quantity(), request.quantity(),
                        params.price(), request.price()
                ))
                .retrieve()
                .body(Map.class);

        if (response == null) {
            log.error("Received null response from order service.");
            throw new RuntimeException("Failed to create order");
        }
        return UUID.fromString(response.get(params.id()).toString());
    }

    @Retryable(retryFor = ResourceAccessException.class,
            maxAttemptsExpression = "${retry.maxAttempts}",
            backoff = @Backoff(delayExpression = "${retry.backoff-delay}"))
    public void confirmOrder(UUID orderId) {
        restClient.patch()
                .uri(url.base() + url.changeStatus(), orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(params.newStatus(), OrderStatus.CONFIRMED))
                .retrieve()
                .toBodilessEntity();
        log.info("Order {} confirmed", orderId);
    }

    @Retryable(retryFor = ResourceAccessException.class,
            maxAttemptsExpression = "${retry.maxAttempts}",
            backoff = @Backoff(delayExpression = "${retry.backoff-delay}"))
    public void cancelOrder(UUID orderId) {
        restClient.patch()
                .uri(url.base() + url.changeStatus(), orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(params.newStatus(), OrderStatus.CANCELLED))
                .retrieve()
                .toBodilessEntity();
        log.info("Compensation: order {} cancelled", orderId);
    }

    @Recover
    public UUID createOrderFallback(ResourceAccessException e, CreateOrderRequest request) {
        log.error("All retries exhausted for createOrder", e);
        throw buildException(e);
    }

    @Recover
    public void confirmOrderFallback(ResourceAccessException e, UUID orderId) {
        log.error("All retries exhausted for confirmOrder orderId={}", orderId, e);
        throw buildException(e);
    }

    @Recover
    public void cancelOrderFallback(ResourceAccessException e, UUID orderId) {
        log.error("All retries exhausted for cancelOrder orderId={}", orderId, e);
        throw buildException(e);
    }

    private ResponseStatusException buildException(ResourceAccessException e) {
        if (e.getCause() instanceof SocketTimeoutException) {
            return new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT,
                    "Order service timed out");
        }
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Order service is unavailable after retries");
    }
}