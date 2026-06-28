package com.microservices.margo.notification_service.core.application.mapper;

import com.microservices.margo.notification_service.core.application.event.OrderCreatedEvent;
import com.microservices.margo.notification_service.core.infrastructure.entity.NotificationEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static com.microservices.margo.notification_service.data.NotificationData.getNotificationEntity;
import static com.microservices.margo.notification_service.data.NotificationData.getOrderCreatedEvent;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NotificationMapper tests")
class NotificationMapperTest {

    private final NotificationMapper mapper = Mappers.getMapper(NotificationMapper.class);

    @Test
    void toEntity_shouldMapEventToEntity() {
        // Arrange
        OrderCreatedEvent event = getOrderCreatedEvent();
        NotificationEntity expected = getNotificationEntity();

        // Act
        NotificationEntity result = mapper.toEntity(event);

        // Assert
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void toEntity_ifEventIsNull_shouldReturnNull() {
        // Act & Assert
        assertThat(mapper.toEntity(null)).isNull();
    }
}