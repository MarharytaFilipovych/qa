package com.microservices.margo.order_service.contract;

import com.microservices.margo.order_service.core.domain.OrderStatus;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.microservices.margo.order_service.data.Constants.SLASH;
import static com.microservices.margo.order_service.data.OrderData.TOO_LONG_ITEM_NAME;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

class OrderIT extends BaseIT {

    private static final String CREATE_ORDER_JSON = "integration/request/create_order.json";
    private static final String CANCEL_ORDER_JSON = "integration/request/cancel_order.json";
    private static final String DELIVER_ORDER_JSON = "integration/request/deliver_order.json";
    private static final String CONFIRM_ORDER_JSON = "integration/request/confirm_order.json";
    private static final String USER_JSON = "integration/user-service/user.json";
    private static final String USERS_PATH = "/api/users/.*";

    @Test
    @SneakyThrows
    void createOrder_whenUserExists_shouldReturn201WithOrderBody() {
        // Arrange
        stubUserExists();

        String requestBody = getFileContent(CREATE_ORDER_JSON);

        // Act & Assert
        var response = createOrderSuccess(requestBody);

        assertThat(response.getHeader("Location")).contains(ORDERS_URL + SLASH + getIdFromResponse(response));

        JSONAssert.assertEquals(requestBody,
                response.getBody().asString(),
                JSONCompareMode.LENIENT);

        assertThat(response.jsonPath().getString("id")).isNotNull();
        assertThat(response.jsonPath().getString("createdAt")).isNotNull();

        userServiceWiremock.verifyThat(getRequestedFor(urlPathMatching(USERS_PATH)));
    }

    @Test
    @SneakyThrows
    void createOrder_whenOwnerUserIdIsNull_shouldReturn400() {
        // Arrange
        stubUserExists();
        String requestBody = withField(getFileContent(CREATE_ORDER_JSON), "ownerUserId", null);

        // Act & Assert
        createOrderFailure(requestBody);
    }

    @ParameterizedTest
    @SneakyThrows
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\n", })
    void createOrder_whenItemNameIsBlank_shouldReturn400(String itemName) {
        // Arrange
        stubUserExists();
        String requestBody = withField(getFileContent(CREATE_ORDER_JSON), "itemName", itemName);

        // Act & Assert
        createOrderFailure(requestBody);
    }

    @Test
    void createOrder_whenItemNameIsTooLong_shouldReturn400() {
        // Arrange
        stubUserExists();
        String requestBody = withField(getFileContent(CREATE_ORDER_JSON), "itemName", TOO_LONG_ITEM_NAME);

        // Act & Assert
        createOrderFailure(requestBody);
    }

    @Test
    @SneakyThrows
    void createOrder_whenPriceIsNull_shouldReturn400() {
        // Arrange
        stubUserExists();
        String requestBody = withField(getFileContent(CREATE_ORDER_JSON), "price", (BigDecimal) null);

        // Act & Assert
        createOrderFailure(requestBody);
    }

    @Test
    @SneakyThrows
    void createOrder_whenPriceIsNegative_shouldReturn400() {
        // Arrange
        stubUserExists();
        String requestBody = withField(getFileContent(CREATE_ORDER_JSON), "price", new BigDecimal("-1"));

        // Act & Assert
        createOrderFailure(requestBody);
    }

    @Test
    @SneakyThrows
    void createOrder_whenQuantityIsZero_shouldReturn400() {
        // Arrange
        stubUserExists();
        String requestBody = withField(getFileContent(CREATE_ORDER_JSON), "quantity", 0);

        // Act & Assert
        createOrderFailure(requestBody);
    }

    @Test
    @SneakyThrows
    void createOrder_whenUserNotFound_shouldReturn404() {
        // Arrange
        userServiceWiremock.register(
                get(urlPathMatching(USERS_PATH))
                        .willReturn(aResponse()
                                .withStatus(HttpStatus.NOT_FOUND.value())
                                .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                                .withBody("{\"message\":\"User not found\"}")));

        String requestBody = getFileContent(CREATE_ORDER_JSON);

        // Act & Assert
        createOrderFailure(requestBody);
        userServiceWiremock.verifyThat(getRequestedFor(urlPathMatching(USERS_PATH)));
    }

    @Test
    @SneakyThrows
    void createOrder_thenGetById_shouldReturnSameOrderData() {
        // Arrange
        stubUserExists();

        String requestBody = getFileContent(CREATE_ORDER_JSON);
        var createResponse = createOrderSuccess(requestBody);
        UUID orderId = getIdFromResponse(createResponse);

        // Act
        var getResponse = given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .get(ORDERS_URL + SLASH + orderId);

        // Assert
        assertThat(getResponse.getStatusCode()).isEqualTo(200);
        JSONAssert.assertEquals(requestBody,
                getResponse.getBody().asString(),
                JSONCompareMode.LENIENT);
    }

    @Test
    @SneakyThrows
    void updateOrderStatus_pendingToConfirmedToDelivered_shouldSucceed() {
        // Arrange
        stubUserExists();
        UUID orderId = getIdFromResponse(createOrderSuccess(getFileContent(CREATE_ORDER_JSON)));

        // Act & Assert
        updateOrderStatusSuccess(orderId, getFileContent(CONFIRM_ORDER_JSON));
        assertOrderStatus(orderId, OrderStatus.CONFIRMED);
        updateOrderStatusSuccess(orderId, getFileContent(DELIVER_ORDER_JSON));
        assertOrderStatus(orderId, OrderStatus.DELIVERED);
    }

    @Test
    @SneakyThrows
    void updateOrderStatus_pendingToCancelled_shouldSucceed() {
        // Arrange
        stubUserExists();
        UUID orderId = getIdFromResponse(createOrderSuccess(getFileContent(CREATE_ORDER_JSON)));

        // Act & Assert
        updateOrderStatusSuccess(orderId, getFileContent(CANCEL_ORDER_JSON));
        assertOrderStatus(orderId, OrderStatus.CANCELLED);
    }

    @Test
    @SneakyThrows
    void updateOrderStatus_illegalTransition_shouldReturn400() {
        // Arrange
        stubUserExists();
        UUID orderId = getIdFromResponse(createOrderSuccess(getFileContent(CREATE_ORDER_JSON)));

        // Act & Assert
        updateOrderStatusFailure(orderId, getFileContent(DELIVER_ORDER_JSON));
    }

    @ParameterizedTest
    @ValueSource(strings = {CANCEL_ORDER_JSON, DELIVER_ORDER_JSON, CONFIRM_ORDER_JSON})
    @SneakyThrows
    void updateOrderStatus_onDeliveredOrder_shouldReturn400(String requestBody) {
        // Arrange
        stubUserExists();
        UUID orderId = getIdFromResponse(createOrderSuccess(getFileContent(CREATE_ORDER_JSON)));

        updateOrderStatusSuccess(orderId, getFileContent(CONFIRM_ORDER_JSON));
        updateOrderStatusSuccess(orderId, getFileContent(DELIVER_ORDER_JSON));

        // Act & Assert
        updateOrderStatusFailure(orderId, getFileContent(requestBody));
    }

    private void stubUserExists() {
        userServiceWiremock.register(
                get(urlPathMatching(USERS_PATH))
                        .willReturn(aResponse()
                                .withStatus(HttpStatus.OK.value())
                                .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                                .withBody(getFileContent(USER_JSON))));
    }

    @SneakyThrows
    private void assertOrderStatus(UUID orderId, OrderStatus expected) {
        var response = given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .get(ORDERS_URL + SLASH + orderId);

        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getString("status")).isEqualTo(expected.name());
    }
}