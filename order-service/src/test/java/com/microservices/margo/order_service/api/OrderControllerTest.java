package com.microservices.margo.order_service.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservices.margo.order_service.core.application.request.CreateOrderRequest;
import com.microservices.margo.order_service.core.application.usecase.CreateOrderUseCase;
import com.microservices.margo.order_service.core.application.usecase.GetOrderUseCase;
import com.microservices.margo.order_service.core.application.usecase.UpdateOrderStatusUseCase;
import com.microservices.margo.order_service.core.domain.Order;
import com.microservices.margo.order_service.core.domain.OrderStatus;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean CreateOrderUseCase createOrder;
    @MockBean GetOrderUseCase getOrder;
    @MockBean UpdateOrderStatusUseCase updateStatus;

    private Order sampleOrder(UUID id) {
        return Order.builder()
                .id(id)
                .itemName("Latte")
                .quantity(2)
                .price(BigDecimal.valueOf(5.99))
                .ownerUserId(UUID.randomUUID())
                .status(OrderStatus.PENDING)
                .build();
    }

    // --- POST /orders ---

    @Test
    void create_returns201WithLocation() throws Exception {
        UUID id = UUID.randomUUID();
        Order order = sampleOrder(id);
        when(createOrder.execute(any())).thenReturn(order);

        CreateOrderRequest req = new CreateOrderRequest("Latte", 2, BigDecimal.valueOf(5.99), UUID.randomUUID());

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString(id.toString())));
    }

    @Test
    void create_returns400_whenItemNameBlank() throws Exception {
        String body = """
                {"itemName":"","quantity":1,"price":5.99,"ownerUserId":"%s"}
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_returns400_whenQuantityZero() throws Exception {
        String body = """
                {"itemName":"Latte","quantity":0,"price":5.99,"ownerUserId":"%s"}
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_returns400_whenPriceMissing() throws Exception {
        String body = """
                {"itemName":"Latte","quantity":1,"ownerUserId":"%s"}
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_returns400_whenOwnerUserIdMissing() throws Exception {
        String body = """
                {"itemName":"Latte","quantity":1,"price":5.99}
                """;

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // --- GET /orders/{id} ---

    @Test
    void getById_returns200WithOrder() throws Exception {
        UUID id = UUID.randomUUID();
        when(getOrder.execute(id)).thenReturn(sampleOrder(id));

        mockMvc.perform(get("/orders/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void getById_returns404_whenNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(getOrder.execute(id)).thenThrow(new EntityNotFoundException("Order with id: " + id + " was not found!"));

        mockMvc.perform(get("/orders/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void getById_returns400_whenIdNotUuid() throws Exception {
        mockMvc.perform(get("/orders/not-a-uuid"))
                .andExpect(status().isBadRequest());
    }

    // --- PATCH /orders/{id}/status ---

    @Test
    void updateStatus_returns204() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(updateStatus).execute(eq(id), any());

        String body = """
                {"newStatus":"CONFIRMED"}
                """;

        mockMvc.perform(patch("/orders/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());
    }

    @Test
    void updateStatus_returns400_whenStatusNull() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(patch("/orders/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateStatus_returns400_whenInvalidTransition() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new IllegalStateException("Cannot change order status from PENDING to DELIVERED"))
                .when(updateStatus).execute(eq(id), any());

        String body = """
                {"newStatus":"DELIVERED"}
                """;

        mockMvc.perform(patch("/orders/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}