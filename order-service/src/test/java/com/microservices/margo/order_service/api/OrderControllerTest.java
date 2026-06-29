package com.microservices.margo.order_service.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservices.margo.order_service.core.application.request.CreateOrderRequest;
import com.microservices.margo.order_service.core.application.request.UpdateOrderStatusRequest;
import com.microservices.margo.order_service.core.application.usecase.CreateOrderUseCase;
import com.microservices.margo.order_service.core.application.usecase.GetOrderUseCase;
import com.microservices.margo.order_service.core.application.usecase.UpdateOrderStatusUseCase;
import com.microservices.margo.order_service.core.domain.Order;
import com.microservices.margo.order_service.core.domain.OrderStatus;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;

import static com.microservices.margo.order_service.data.Constants.SLASH;
import static com.microservices.margo.order_service.data.OrderData.TOO_LONG_ITEM_NAME;
import static com.microservices.margo.order_service.data.OrderData.createOrderRequest;
import static com.microservices.margo.order_service.data.OrderData.getOrder;
import static com.microservices.margo.order_service.data.OrderData.updateStatusRequest;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("OrderController tests")
class OrderControllerTest {

    private static final String ORDERS_PATH = "/orders";
    private static final String STATUS_SUB_PATH = "/status";

    private static final Order ORDER = getOrder();
    private static final CreateOrderRequest CREATE_ORDER_REQUEST = createOrderRequest();

    @MockitoBean
    private CreateOrderUseCase createOrderUseCase;

    @MockitoBean
    private GetOrderUseCase getOrderUseCase;

    @MockitoBean
    private UpdateOrderStatusUseCase updateOrderStatusUseCase;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void create_shouldReturnCreatedOrderWithLocation() throws Exception {
        // Arrange
        when(createOrderUseCase.execute(CREATE_ORDER_REQUEST)).thenReturn(ORDER);

        // Act & Assert
        andExpectOrder(
                mockMvc.perform(post(ORDERS_PATH)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(CREATE_ORDER_REQUEST)))
                        .andExpect(status().isCreated())
                        .andExpect(header().string("Location", containsString(ORDERS_PATH + "/" + ORDER.id())))
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    void create_shouldReturnBadRequestWhenItemNameIsAbsent(String itemName) throws Exception {
        // Arrange
        CreateOrderRequest request = CREATE_ORDER_REQUEST.toBuilder().itemName(itemName).build();

        // Act & Assert
        assertBadRequest(request, "itemName: Item name must be specified.");
    }

    @Test
    void create_shouldReturnBadRequestWhenItemNameIsTooLong() throws Exception {
        // Arrange
        CreateOrderRequest request = CREATE_ORDER_REQUEST.toBuilder().itemName(TOO_LONG_ITEM_NAME).build();

        // Act & Assert
        assertBadRequest(request, "itemName: Item name must consist at most of 255 symbols");
    }

    @Test
    void create_shouldReturnBadRequestWhenQuantityIsZero() throws Exception {
        // Arrange
        CreateOrderRequest request = CREATE_ORDER_REQUEST.toBuilder().quantity(0).build();

        // Act & Assert
        assertBadRequest(request, "quantity: Quantity must be at least 1.");
    }

    @Test
    void create_shouldReturnBadRequestWhenPriceIsNull() throws Exception {
        // Arrange
        CreateOrderRequest request = CREATE_ORDER_REQUEST.toBuilder().price(null).build();

        // Act & Assert
        assertBadRequest(request, "price: Price name must be specified.");
    }

    @Test
    void create_shouldReturnBadRequestWhenPriceIsNegative() throws Exception {
        // Arrange
        CreateOrderRequest request = CREATE_ORDER_REQUEST.toBuilder().price(new BigDecimal("-1")).build();

        // Act & Assert
        assertBadRequest(request, "price: Price cannot be negative.");
    }

    @Test
    void create_shouldReturnBadRequestWhenOwnerUserIdIsNull() throws Exception {
        // Arrange
        CreateOrderRequest request = CREATE_ORDER_REQUEST.toBuilder().ownerUserId(null).build();

        // Act & Assert
        assertBadRequest(request, "ownerUserId: Customer id is required.");
    }

    @Test
    void getById_shouldReturnOrder() throws Exception {
        // Arrange
        when(getOrderUseCase.execute(ORDER.id())).thenReturn(ORDER);

        // Act & Assert
        andExpectOrder(
                mockMvc.perform(get(ORDERS_PATH + SLASH + ORDER.id()))
                        .andExpect(status().isOk())
        );
    }

    @Test
    void getById_shouldReturn404WhenNotFound() throws Exception {
        // Arrange
        String msg = "Order not found";
        when(getOrderUseCase.execute(ORDER.id())).thenThrow(new EntityNotFoundException(msg));

        // Act & Assert
        mockMvc.perform(get(ORDERS_PATH + SLASH + ORDER.id()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(msg));
    }

    @Test
    @DisplayName("getById should return 400 when id is not a valid UUID")
    void getById_shouldReturn400WhenIdIsInvalid() throws Exception {
        // Act & Assert
        mockMvc.perform(get(ORDERS_PATH + "/not-a-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateStatus_shouldReturnNoContent() throws Exception {
        // Arrange
        UpdateOrderStatusRequest request = updateStatusRequest(OrderStatus.CONFIRMED);

        // Act & Assert
        mockMvc.perform(patch(ORDERS_PATH + SLASH + ORDER.id() + STATUS_SUB_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    void updateStatus_shouldReturn400WhenNewStatusIsNull() throws Exception {
        // Arrange
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(null);

        // Act & Assert
        assertBadRequest(request, "newStatus: must not be null");
    }

    @Test
    void updateStatus_shouldReturn404WhenOrderNotFound() throws Exception {
        // Arrange
        UpdateOrderStatusRequest request = updateStatusRequest(OrderStatus.CONFIRMED);
        String errorMessage = "Order not found";
        doThrow(new EntityNotFoundException(errorMessage))
                .when(updateOrderStatusUseCase).execute(ORDER.id(), request);

        // Act & Assert
        assertNegativeStatus(request, errorMessage, HttpStatus.NOT_FOUND);
    }

    @Test
    void updateStatus_shouldReturn400WhenTransitionIsIllegal() throws Exception {
        // Arrange
        UpdateOrderStatusRequest request = updateStatusRequest(OrderStatus.DELIVERED);
        String errorMessage = "Cannot change order status from PENDING to DELIVERED";
        doThrow(new IllegalStateException(errorMessage))
                .when(updateOrderStatusUseCase).execute(ORDER.id(), request);

        // Act & Assert
        assertBadRequest(request, errorMessage);
    }

    private void assertBadRequest(UpdateOrderStatusRequest request, String expectedMessage) throws Exception {
       assertNegativeStatus(request, expectedMessage, HttpStatus.BAD_REQUEST);
    }
    private void assertNegativeStatus(UpdateOrderStatusRequest request, String expectedMessage, HttpStatus httpStatus) throws Exception {
        mockMvc.perform(patch(ORDERS_PATH + SLASH + ORDER.id() + STATUS_SUB_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is(httpStatus.value()))
                .andExpect(jsonPath("$.message").value(expectedMessage));
    }

    private void assertBadRequest(CreateOrderRequest request, String expectedMessage) throws Exception {
        mockMvc.perform(post(ORDERS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(expectedMessage));
    }

    private void andExpectOrder(ResultActions actions) throws Exception {
        actions.andExpect(jsonPath("$.id").value(ORDER.id().toString()))
                .andExpect(jsonPath("$.itemName").value(ORDER.itemName()))
                .andExpect(jsonPath("$.quantity").value(ORDER.quantity()))
                .andExpect(jsonPath("$.ownerUserId").value(ORDER.ownerUserId().toString()));
    }
}