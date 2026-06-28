package com.microservices.margo.order_service.core.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static com.microservices.margo.order_service.data.OrderData.getOrder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Order domain tests")
class OrderTest {
    private static final Order ORDER = getOrder();

    @ParameterizedTest
    @MethodSource("com.microservices.margo.order_service.data.OrderData#validPendingTransitions")
    void changeStatus_shouldTransitionFromPending(OrderStatus next) {
        // Act
        Order updated = ORDER.changeStatus(next);

        // Assert
        assertThat(updated.status()).isEqualTo(next);
    }

    @ParameterizedTest
    @MethodSource("com.microservices.margo.order_service.data.OrderData#validConfirmedTransitions")
    void changeStatus_shouldTransitionFromConfirmed(OrderStatus next) {
        // Arrange
        Order order = ORDER.toBuilder().status(OrderStatus.CONFIRMED).build();

        // Act
        Order updated = order.changeStatus(next);

        // Assert
        assertThat(updated.status()).isEqualTo(next);
    }

    @ParameterizedTest
    @MethodSource("com.microservices.margo.order_service.data.OrderData#terminalStatuses")
    void changeStatus_shouldThrowWhenInTerminalState(OrderStatus terminal) {
        // Arrange
        Order order = ORDER.toBuilder().status(terminal).build();

        // Act & Assert
        assertThatThrownBy(() -> order.changeStatus(OrderStatus.CONFIRMED))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot change order status from " + terminal);
    }

    @Test
    void changeStatus_shouldThrowWhenTransitionIsInvalid() {
         // Act & Assert
        assertThatThrownBy(() -> ORDER.changeStatus(OrderStatus.DELIVERED))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot change order status from PENDING to DELIVERED");
    }
}