package com.microservices.margo.order_service.data;

import com.microservices.margo.order_service.core.application.request.CreateOrderRequest;
import com.microservices.margo.order_service.core.application.request.UpdateOrderStatusRequest;
import com.microservices.margo.order_service.core.domain.Order;
import com.microservices.margo.order_service.core.domain.OrderStatus;
import com.microservices.margo.order_service.core.infrastructure.entity.OrderEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.stream.Stream;

public final class OrderData {

    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID OWNER_USER_ID = UUID.randomUUID();
    private static final String ITEM_NAME = "Latte";
    private static final int QUANTITY = 2;
    private static final BigDecimal PRICE = new BigDecimal("5.99");
    private static final LocalDateTime CREATED_AT = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

    public static final String TOO_LONG_ITEM_NAME = "a".repeat(256);

    public static Order getOrder() {
        return Order.builder()
                .id(ORDER_ID)
                .itemName(ITEM_NAME)
                .quantity(QUANTITY)
                .price(PRICE)
                .ownerUserId(OWNER_USER_ID)
                .status(OrderStatus.PENDING)
                .createdAt(CREATED_AT)
                .build();
    }

    public static CreateOrderRequest createOrderRequest() {
        return new CreateOrderRequest(ITEM_NAME, QUANTITY, PRICE, OWNER_USER_ID);
    }

    public static OrderEntity getOrderEntity() {
        return OrderEntity.builder()
                .id(ORDER_ID)
                .itemName(ITEM_NAME)
                .quantity(QUANTITY)
                .price(PRICE)
                .ownerUserId(OWNER_USER_ID)
                .status(OrderStatus.PENDING)
                .createdAt(CREATED_AT)
                .build();
    }

    public static UpdateOrderStatusRequest updateStatusRequest(OrderStatus status) {
        return new UpdateOrderStatusRequest(status);
    }

    static Stream<OrderStatus> validPendingTransitions() {
        return Stream.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED);
    }

    static Stream<OrderStatus> validConfirmedTransitions() {
        return Stream.of(OrderStatus.DELIVERED, OrderStatus.CANCELLED);
    }

    static Stream<OrderStatus> terminalStatuses() {
        return Stream.of(OrderStatus.DELIVERED, OrderStatus.CANCELLED);
    }
}