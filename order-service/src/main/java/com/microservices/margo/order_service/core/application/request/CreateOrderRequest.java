package com.microservices.margo.order_service.core.application.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

import static com.microservices.margo.order_service.core.domain.validation.ValidationConstants.MAX_NAME_LENGTH;

@Builder(toBuilder = true)
public record CreateOrderRequest(
        @NotBlank(message = "Item name must be specified.")
        @Size(max = MAX_NAME_LENGTH, message = "Item name must consist at most of 255 symbols")
        String itemName,

        @Positive(message = "Quantity must be at least 1.")
        int quantity,

        @NotNull(message = "Price name must be specified.")
        @PositiveOrZero(message = "Price cannot be negative.")
        BigDecimal price,

        @NotNull(message = "Customer id is required.")
        UUID ownerUserId
) {}