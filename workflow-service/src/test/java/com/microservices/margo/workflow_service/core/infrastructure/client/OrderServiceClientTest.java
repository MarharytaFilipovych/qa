package com.microservices.margo.workflow_service.core.infrastructure.client;

import com.microservices.margo.workflow_service.core.application.request.CreateOrderRequest;
import com.microservices.margo.workflow_service.core.domain.OrderStatus;
import com.microservices.margo.workflow_service.core.infrastructure.config.OrderServiceProperties;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.UUID;

import static com.microservices.margo.workflow_service.data.WorkflowData.createOrderRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatNoException;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("OrderServiceClient tests")
@ExtendWith(MockitoExtension.class)
class OrderServiceClientTest {

    private static final String BASE_URL = "http://order-service";
    private static final String CREATE_ORDER_PATH = "/api/orders";
    private static final String CHANGE_STATUS_PATH = "/api/orders/{id}/status";
    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final CreateOrderRequest REQUEST = createOrderRequest();

    private static final OrderServiceProperties.Url URL =
            new OrderServiceProperties.Url(BASE_URL, CREATE_ORDER_PATH, CHANGE_STATUS_PATH);
    private static final OrderServiceProperties.Params PARAMS =
            new OrderServiceProperties.Params("ownerUserId", "itemName", "quantity", "price", "newStatus", "id");
    private static final OrderServiceProperties PROPERTIES =
            new OrderServiceProperties(URL, PARAMS);

    @Mock
    private RestClient restClient;

    private OrderServiceClient client;

    @BeforeEach
    void setUp() {
        client = new OrderServiceClient(restClient, PROPERTIES);
    }

    @Test
    void createOrder_shouldReturnOrderIdFromResponse() {
        // Arrange
        mockPostResponse(Map.of(PARAMS.id(), ORDER_ID));

        // Act
        UUID result = client.createOrder(REQUEST);

        // Assert
        assertThat(result).isEqualTo(ORDER_ID);
    }

    @Test
    void createOrder_shouldThrowWhenResponseIsNull() {
        // Arrange
        mockPostResponse(null);


        // Act & Assert
        assertThatThrownBy(() -> client.createOrder(REQUEST))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to create order");
    }

    @Test
    void confirmOrder_shouldSendPatchRequestWithConfirmedStatus() {
        // Arrange
        mockPatchResponse(OrderStatus.CONFIRMED);

        // Act & Assert
        assertThatNoException().isThrownBy(() -> client.confirmOrder(ORDER_ID));
        verify(restClient).patch();
    }

    @Test
    void cancelOrder_shouldSendPatchRequestWithCancelledStatus() {
        // Arrange
        mockPatchResponse(OrderStatus.CANCELLED);

        // Act & Assert
        assertThatNoException().isThrownBy(() -> client.cancelOrder(ORDER_ID));
        verify(restClient).patch();
    }

    @Test
    void createOrderFallback_shouldThrow503WhenCauseIsNotSocketTimeout() {
        // Arrange
        ResourceAccessException e = new ResourceAccessException("connection refused");

        // Act & Assert
        assertFallbackStatus(() -> client.createOrderFallback(e, REQUEST), HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void createOrderFallback_shouldThrow504WhenCauseIsSocketTimeout() {
        // Arrange
        ResourceAccessException e = new ResourceAccessException("timeout", new SocketTimeoutException());

        // Act & Assert
        assertFallbackStatus(() -> client.createOrderFallback(e, REQUEST), HttpStatus.GATEWAY_TIMEOUT);
    }

    @Test
    void confirmOrderFallback_shouldThrow503WhenCauseIsNotSocketTimeout() {
        // Arrange
        ResourceAccessException e = new ResourceAccessException("connection refused");

        // Act & Assert
        assertFallbackStatus(() -> client.confirmOrderFallback(e, ORDER_ID), HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void confirmOrderFallback_shouldThrow504WhenCauseIsSocketTimeout() {
        // Arrange
        ResourceAccessException e = new ResourceAccessException("timeout", new SocketTimeoutException());

        // Act & Assert
        assertFallbackStatus(() -> client.confirmOrderFallback(e, ORDER_ID), HttpStatus.GATEWAY_TIMEOUT);
    }

    @Test
    void cancelOrderFallback_shouldThrow503WhenCauseIsNotSocketTimeout() {
        // Arrange
        ResourceAccessException e = new ResourceAccessException("connection refused");

        // Act & Assert
        assertFallbackStatus(() -> client.cancelOrderFallback(e, ORDER_ID), HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void cancelOrderFallback_shouldThrow504WhenCauseIsSocketTimeout() {
        // Arrange
        ResourceAccessException e = new ResourceAccessException("timeout", new SocketTimeoutException());

        // Act & Assert
        assertFallbackStatus(() -> client.cancelOrderFallback(e, ORDER_ID), HttpStatus.GATEWAY_TIMEOUT);
    }

    private void assertFallbackStatus(ThrowableAssert.ThrowingCallable fallback, HttpStatus expectedStatus) {
        assertThatThrownBy(fallback)
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(expectedStatus));
    }

    private void mockPatchResponse(OrderStatus status) {
        RestClient.RequestBodyUriSpec uriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        doReturn(uriSpec).when(restClient).patch();
        doReturn(bodySpec).when(uriSpec).uri(BASE_URL + CHANGE_STATUS_PATH, ORDER_ID);
        doReturn(bodySpec).when(bodySpec).contentType(MediaType.APPLICATION_JSON);
        doReturn(bodySpec).when(bodySpec).body(Map.of("newStatus", status));
        doReturn(responseSpec).when(bodySpec).retrieve();
        doReturn(null).when(responseSpec).toBodilessEntity();
    }

    private void mockPostResponse(Map<String, UUID> expectedResponse) {
        RestClient.RequestBodyUriSpec uriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        Map<String, Object> expectedBody = Map.of(
                PARAMS.ownerUserId(), REQUEST.ownerUserId(),
                PARAMS.itemName(), REQUEST.itemName(),
                PARAMS.quantity(), REQUEST.quantity(),
                PARAMS.price(), REQUEST.price()
        );

        doReturn(uriSpec).when(restClient).post();
        doReturn(bodySpec).when(uriSpec).uri(BASE_URL + CREATE_ORDER_PATH);
        doReturn(bodySpec).when(bodySpec).contentType(MediaType.APPLICATION_JSON);
        doReturn(bodySpec).when(bodySpec).body(expectedBody);
        doReturn(responseSpec).when(bodySpec).retrieve();
        doReturn(expectedResponse).when(responseSpec).body(Map.class);
    }
}