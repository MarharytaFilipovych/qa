package com.microservices.margo.order_service.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservices.margo.order_service.core.application.request.CreateOrderRequest;
import com.microservices.margo.order_service.core.application.request.UpdateOrderStatusRequest;
import com.microservices.margo.order_service.core.domain.OrderStatus;
import com.microservices.margo.order_service.core.infrastructure.client.UserValidationClient;
import com.microservices.margo.order_service.core.infrastructure.publisher.OrderEventPublisher;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;

import static com.microservices.margo.order_service.data.Constants.CORRELATION_ID;
import static com.microservices.margo.order_service.data.Constants.CORRELATION_ID_HEADER;
import static com.microservices.margo.order_service.data.Constants.MESSAGE_IN_PAYLOAD;
import static com.microservices.margo.order_service.data.Constants.ORDER_PATH;
import static com.microservices.margo.order_service.data.Constants.SLASH;
import static com.microservices.margo.order_service.data.OrderData.ORDER_ID;
import static com.microservices.margo.order_service.data.OrderData.createOrderRequest;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("OrderService Security Tests")
class OrderSecurityIT {

    private static final String STATUS_PATH = "/status";
    private static final String UUID_PATTERN =
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

    @MockitoBean
    private UserValidationClient userValidationClient;

    @MockitoBean
    private OrderEventPublisher orderEventPublisher;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String json(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    private String createOrderAndGetId(CreateOrderRequest request) throws Exception {
        MvcResult result = mockMvc.perform(post(ORDER_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asText();
    }

    @Test
    @SneakyThrows
    @DisplayName("Should propagate existing X-Correlation-Id through the full filter chain")
    void shouldPropagateExistingCorrelationId() {
        // Act & Assert
        mockMvc.perform(get(ORDER_PATH + SLASH + ORDER_ID)
                        .header(CORRELATION_ID_HEADER, CORRELATION_ID))
                .andExpect(header().string(CORRELATION_ID_HEADER, CORRELATION_ID));
    }

    @Test
    @SneakyThrows
    @DisplayName("Should generate a valid UUID X-Correlation-Id when none is provided")
    void shouldGenerateCorrelationIdWhenAbsent() {
        // Act & Assert
        mockMvc.perform(get(ORDER_PATH + SLASH + ORDER_ID))
                .andExpect(header().string(CORRELATION_ID_HEADER, matchesPattern(UUID_PATTERN)));
    }

    @Test
    @SneakyThrows
    @DisplayName("Should generate a fresh UUID correlation ID when a blank value is sent")
    void shouldReplaceBlankCorrelationId() {
        // Act & Assert
        mockMvc.perform(get(ORDER_PATH + SLASH + ORDER_ID)
                        .header(CORRELATION_ID_HEADER, "   "))
                .andExpect(header().string(CORRELATION_ID_HEADER, matchesPattern(UUID_PATTERN)));
    }

    @Test
    @SneakyThrows
    @DisplayName("X-Correlation-Id must be present even on validation error responses")
    void shouldReturnCorrelationIdOnErrorResponse() {
        // Act & Assert
        mockMvc.perform(post(ORDER_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().exists(CORRELATION_ID_HEADER));
    }

    @Test
    @SneakyThrows
    @DisplayName("X-Correlation-Id must be present on 404 error responses")
    void shouldReturnCorrelationIdOn404Response() {
        // Act & Assert
        mockMvc.perform(get(ORDER_PATH + SLASH + ORDER_ID))
                .andExpect(status().isNotFound())
                .andExpect(header().exists(CORRELATION_ID_HEADER));
    }

    @ParameterizedTest
    @SneakyThrows
    @ValueSource(strings = {
            "'; DROP TABLE orders; --",
            "' OR '1'='1",
            "' UNION SELECT id, item_name, quantity, price, owner_user_id, status, created_at FROM orders--"
    })
    @DisplayName("SQL-injection payload in itemName is stored as literal text, not executed")
    void sqlInjectionInItemNameIsStoredSafely(String maliciousInput) {
        // Arrange
        CreateOrderRequest request = new CreateOrderRequest(
                maliciousInput, 1, BigDecimal.ONE, UUID.randomUUID());

        String id = createOrderAndGetId(request);

        // Act & Assert
        mockMvc.perform(get(ORDER_PATH + SLASH + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemName").value(maliciousInput));
    }

    @Test
    @SneakyThrows
    @DisplayName("SQL-injection in one order's itemName does not corrupt another order's data")
    void sqlInjectionDoesNotCorruptOtherRows() {
        // Arrange
        CreateOrderRequest victim = createOrderRequest();
        String victimId = createOrderAndGetId(victim);

        // Act
        CreateOrderRequest attacker = new CreateOrderRequest(
                "' UNION SELECT id,item_name,item_name,price,owner_user_id,status,created_at FROM orders--",
                1, BigDecimal.ONE, UUID.randomUUID());
        mockMvc.perform(post(ORDER_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(attacker)))
                .andExpect(status().isCreated());

        // Assert
        mockMvc.perform(get(ORDER_PATH + SLASH + victimId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemName").value(victim.itemName()))
                .andExpect(jsonPath("$.quantity").value(victim.quantity()));
    }

    @ParameterizedTest
    @SneakyThrows
    @ValueSource(strings = {
            "<script>alert('xss')</script>",
            "<img src=x onerror=alert(1)>",
            "javascript:alert(document.cookie)"
    })
    @DisplayName("Error responses must never reflect raw XSS payloads back to the caller")
    void errorResponsesMustNotReflectXssPayloads(String xssPayload) {
        // Arrange
        CreateOrderRequest request = createOrderRequest().toBuilder()
                .itemName(xssPayload).quantity(0).build();

        // Act & Assert
        mockMvc.perform(post(ORDER_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(MESSAGE_IN_PAYLOAD).value("quantity: Quantity must be at least 1."))
                .andExpect(jsonPath(MESSAGE_IN_PAYLOAD).value(not(containsString(xssPayload))));
    }

    @Test
    @SneakyThrows
    @DisplayName("Should reject PENDING → DELIVERED transition with 400 (invalid transition)")
    void shouldRejectInvalidStatusTransitionPendingToDelivered() {
        // Arrange
        String id = createOrderAndGetId(createOrderRequest());

        // Act & Assert
        mockMvc.perform(patch(ORDER_PATH + SLASH + id + STATUS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new UpdateOrderStatusRequest(OrderStatus.DELIVERED))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(MESSAGE_IN_PAYLOAD)
                        .value("Cannot change order status from PENDING to DELIVERED"));
    }

    @Test
    @SneakyThrows
    @DisplayName("Should reject update on a terminal (CANCELLED) order with 400")
    void shouldRejectUpdateOnTerminalOrder() {
        // Arrange
        String id = createOrderAndGetId(createOrderRequest());

        // PENDING → CANCELLED (valid)
        mockMvc.perform(patch(ORDER_PATH + SLASH + id + STATUS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new UpdateOrderStatusRequest(OrderStatus.CANCELLED))))
                .andExpect(status().isNoContent());

        // Act & Assert
        // CANCELLED → CONFIRMED (must be rejected — terminal state)
        mockMvc.perform(patch(ORDER_PATH + SLASH + id + STATUS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new UpdateOrderStatusRequest(OrderStatus.CONFIRMED))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(MESSAGE_IN_PAYLOAD)
                        .value("Cannot change order status from CANCELLED to CONFIRMED"));
    }

    @ParameterizedTest
    @SneakyThrows
    @ValueSource(strings = {MediaType.TEXT_PLAIN_VALUE, MediaType.APPLICATION_ATOM_XML_VALUE, MediaType.APPLICATION_CBOR_VALUE})
    @DisplayName("Should return 415 when unsupported content type is sent")
    void shouldReturn415ForWrongContentType(String contentType) {
        // Act & Assert
        mockMvc.perform(post(ORDER_PATH)
                        .contentType(MediaType.valueOf(contentType))
                        .content(json(createOrderRequest())))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath(MESSAGE_IN_PAYLOAD)
                        .value("Content-Type '%s;charset=UTF-8' is not supported".formatted(contentType)));
    }

    @ParameterizedTest
    @SneakyThrows
    @ValueSource(strings = {"{\"itemName\": \"Latte\"", "not-json"})
    @DisplayName("Should return 400 with a safe error message for invalid JSON")
    void shouldReturn400ForInvalidJson(String body) {
        // Act & Assert
        mockMvc.perform(post(ORDER_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(MESSAGE_IN_PAYLOAD).value("Invalid JSON format in request body"));
    }

    @Test
    @SneakyThrows
    @DisplayName("Non-UUID path segment should return 400 with a structured JSON error")
    void nonUuidPathParamShouldReturn400WithStructuredError() {
        // Arrange
        String id = "not-a-uuid";

        // Act & Assert
        mockMvc.perform(get(ORDER_PATH + SLASH + id))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath(MESSAGE_IN_PAYLOAD).value("Invalid value for parameter 'id': %s".formatted(id)));
    }

    @Test
    @SneakyThrows
    @DisplayName("Should return 404 for unknown routes with a structured JSON body")
    void shouldReturn404ForUnknownRoute() {
        // Arrange
        String route = "/unknown";

        // Act & Assert
        mockMvc.perform(get(route))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath(MESSAGE_IN_PAYLOAD).value("No endpoint GET %s".formatted(route)));
    }

    @Test
    @SneakyThrows
    @DisplayName("Should return 405 for unsupported HTTP methods with a structured JSON body")
    void shouldReturn405ForUnsupportedMethod() {
        // Act & Assert
        mockMvc.perform(delete(ORDER_PATH))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath(MESSAGE_IN_PAYLOAD).value("Request method 'DELETE' is not supported"));
    }
}