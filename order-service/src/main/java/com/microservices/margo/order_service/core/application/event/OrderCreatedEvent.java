package com.microservices.margo.order_service.core.application.event;

import java.time.Instant;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID eventId,
        Instant occurredAt,
        String correlationId,
        UUID orderId,
        UUID ownerUserId,
        String payload
) {}
