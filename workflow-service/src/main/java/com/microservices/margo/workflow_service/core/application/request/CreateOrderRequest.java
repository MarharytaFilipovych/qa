package com.microservices.margo.workflow_service.core.application.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

import static com.microservices.margo.workflow_service.core.domain.validation.ValidationConstants.MAX_NAME_LENGTH;

public record CreateOrderRequest(
        @NotNull(message = "ownerUserId is required")
        UUID ownerUserId,

        @NotBlank(message = "itemName is required")
        @Size(max = MAX_NAME_LENGTH, message = "Item name must consist at most of 255 symbols")
        String itemName,

        @Positive(message = "quantity must be at least 1")
        int quantity,

        @NotNull(message = "price is required")
        @PositiveOrZero(message = "price cannot be negative")
        BigDecimal price
) {}