package com.microservices.margo.notification_service.core.application.event;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record OrderCreatedEvent(
        @NotNull UUID eventId,
        @NotNull Instant occurredAt,
        @NotNull String correlationId,
        @NotNull UUID orderId,
        @NotNull UUID ownerUserId,
        String payload
) {}
