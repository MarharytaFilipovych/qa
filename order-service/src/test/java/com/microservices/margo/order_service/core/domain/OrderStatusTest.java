package com.microservices.margo.order_service.core.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class OrderStatusTest {

    @ParameterizedTest
    @CsvSource({
            "PENDING,   CONFIRMED, true",
            "PENDING,   CANCELLED, true",
            "PENDING,   DELIVERED, false",
            "CONFIRMED, DELIVERED, true",
            "CONFIRMED, CANCELLED, true",
            "CONFIRMED, PENDING,   false",
            "DELIVERED, CANCELLED, false",
            "DELIVERED, CONFIRMED, false",
            "CANCELLED, PENDING,   false",
            "CANCELLED, CONFIRMED, false",
    })
    void transitionMatrix(OrderStatus from, OrderStatus to, boolean expected) {
        assertThat(from.canTransitionTo(to)).isEqualTo(expected);
    }
}