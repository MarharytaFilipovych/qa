package com.microservices.margo.order_service.core.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class OrderStatusTest {

    @ParameterizedTest
    @CsvSource({
            "PENDING, CONFIRMED, true",
            "PENDING, CANCELLED, true",
            "PENDING, DELIVERED, false",
            "CONFIRMED, DELIVERED, true",
            "CONFIRMED, CANCELLED, true",
            "CONFIRMED, PENDING, false",
            "DELIVERED, CANCELLED, false",
            "DELIVERED, CONFIRMED, false",
            "CANCELLED, CONFIRMED, false",
            "CANCELLED, DELIVERED, false"
    })
    void canTransitionTo_shouldReturnExpected(OrderStatus from, OrderStatus to, boolean expected) {
        // Act & Assert
        assertThat(from.canTransitionTo(to)).isEqualTo(expected);
    }
}