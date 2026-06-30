package com.microservices.margo.notification_service.core.application.usecase;

import com.microservices.margo.notification_service.core.application.event.OrderCreatedEvent;
import com.microservices.margo.notification_service.core.application.mapper.NotificationMapper;
import com.microservices.margo.notification_service.core.infrastructure.entity.NotificationEntity;
import com.microservices.margo.notification_service.core.infrastructure.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static com.microservices.margo.notification_service.data.NotificationData.getNotificationEntity;
import static com.microservices.margo.notification_service.data.NotificationData.getOrderCreatedEvent;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

@DisplayName("StoreNotificationUseCase tests")
@ExtendWith(MockitoExtension.class)
class StoreNotificationUseCaseTest {

    private static final OrderCreatedEvent EVENT = getOrderCreatedEvent();
    private static final NotificationEntity ENTITY = getNotificationEntity();

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationMapper notificationMapper;

    @InjectMocks
    private StoreNotificationUseCase storeNotificationUseCase;

    @Test
    void execute_shouldMapAndSaveNotification() {
        // Arrange
        when(notificationMapper.toEntity(EVENT)).thenReturn(ENTITY);

        // Act
        storeNotificationUseCase.execute(EVENT);

        // Assert
        verify(notificationMapper).toEntity(EVENT);
        verify(notificationRepository).save(ENTITY);
    }

    @Test
    void execute_shouldSilentlyIgnoreDuplicateEvent() {
        // Arrange
        when(notificationMapper.toEntity(EVENT)).thenReturn(ENTITY);
        doThrow(new DataIntegrityViolationException("duplicate key"))
                .when(notificationRepository).save(ENTITY);

        // Act & Assert
        assertThatNoException().isThrownBy(() -> storeNotificationUseCase.execute(EVENT));
        verify(notificationRepository).save(ENTITY);
    }
}