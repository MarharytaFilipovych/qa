package com.microservices.margo.notification_service.data;

import com.microservices.margo.notification_service.core.application.event.OrderCreatedEvent;
import com.microservices.margo.notification_service.core.infrastructure.entity.NotificationEntity;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public final class NotificationData {

    private static final UUID EVENT_ID = UUID.randomUUID();
    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID OWNER_USER_ID = UUID.randomUUID();
    private static final String CORRELATION_ID = UUID.randomUUID().toString();
    private static final Instant OCCURRED_AT = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    private static final String PAYLOAD = "Order created: Latte x2 @ 5.99";

    public static OrderCreatedEvent getOrderCreatedEvent() {
        return new OrderCreatedEvent(EVENT_ID, OCCURRED_AT, CORRELATION_ID, ORDER_ID, OWNER_USER_ID, PAYLOAD);
    }

    public static NotificationEntity getNotificationEntity() {
        return NotificationEntity.builder()
                .eventId(EVENT_ID)
                .correlationId(CORRELATION_ID)
                .coreItemId(ORDER_ID)
                .ownerUserId(OWNER_USER_ID)
                .payload(PAYLOAD)
                .createdAt(OCCURRED_AT)
                .build();
    }
}