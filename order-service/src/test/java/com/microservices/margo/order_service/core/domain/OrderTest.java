package com.microservices.margo.order_service.core.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    private Order pendingOrder() {
        return Order.builder()
                .id(UUID.randomUUID())
                .itemName("Latte")
                .quantity(2)
                .price(BigDecimal.valueOf(5.99))
                .ownerUserId(UUID.randomUUID())
                .build();
    }

    @Test
    void defaultStatusIsPending() {
        Order order = pendingOrder();
        assertThat(order.status()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void pendingCanTransitionToConfirmed() {
        Order order = pendingOrder().changeStatus(OrderStatus.CONFIRMED);
        assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void pendingCanTransitionToCancelled() {
        Order order = pendingOrder().changeStatus(OrderStatus.CANCELLED);
        assertThat(order.status()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void confirmedCanTransitionToDelivered() {
        Order order = pendingOrder()
                .changeStatus(OrderStatus.CONFIRMED)
                .changeStatus(OrderStatus.DELIVERED);
        assertThat(order.status()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    void confirmedCanTransitionToCancelled() {
        Order order = pendingOrder()
                .changeStatus(OrderStatus.CONFIRMED)
                .changeStatus(OrderStatus.CANCELLED);
        assertThat(order.status()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void pendingCannotTransitionToDelivered() {
        assertThatThrownBy(() -> pendingOrder().changeStatus(OrderStatus.DELIVERED))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PENDING")
                .hasMessageContaining("DELIVERED");
    }

    @Test
    void deliveredIsTerminal() {
        Order delivered = pendingOrder()
                .changeStatus(OrderStatus.CONFIRMED)
                .changeStatus(OrderStatus.DELIVERED);
        assertThatThrownBy(() -> delivered.changeStatus(OrderStatus.CANCELLED))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cancelledIsTerminal() {
        Order cancelled = pendingOrder().changeStatus(OrderStatus.CANCELLED);
        assertThatThrownBy(() -> cancelled.changeStatus(OrderStatus.CONFIRMED))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void changeStatusReturnsNewInstance() {
        Order original = pendingOrder();
        Order updated = original.changeStatus(OrderStatus.CONFIRMED);
        assertThat(original.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(updated.status()).isEqualTo(OrderStatus.CONFIRMED);
    }
}