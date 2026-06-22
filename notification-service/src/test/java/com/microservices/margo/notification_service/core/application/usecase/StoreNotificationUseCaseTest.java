package com.microservices.margo.notification_service.core.application.usecase;

import com.microservices.margo.notification_service.core.application.event.OrderCreatedEvent;
import com.microservices.margo.notification_service.core.application.mapper.NotificationMapper;
import com.microservices.margo.notification_service.core.infrastructure.entity.NotificationEntity;
import com.microservices.margo.notification_service.core.infrastructure.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StoreNotificationUseCaseTest {

    @Mock NotificationRepository notificationRepository;
    @Mock NotificationMapper notificationMapper;

    @InjectMocks StoreNotificationUseCase useCase;

    private OrderCreatedEvent sampleEvent() {
        return new OrderCreatedEvent(
                UUID.randomUUID(),
                Instant.now(),
                "corr-123",
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Latte x2"
        );
    }

    @Test
    void execute_savesNotification() {
        OrderCreatedEvent event = sampleEvent();
        NotificationEntity entity = new NotificationEntity();

        when(notificationMapper.toEntity(event)).thenReturn(entity);

        useCase.execute(event);

        verify(notificationRepository).save(entity);
    }

    @Test
    void execute_ignoresDuplicate_whenDataIntegrityViolation() {
        OrderCreatedEvent event = sampleEvent();
        NotificationEntity entity = new NotificationEntity();

        when(notificationMapper.toEntity(event)).thenReturn(entity);
        when(notificationRepository.save(entity)).thenThrow(new DataIntegrityViolationException("dup"));

        // should NOT throw
        useCase.execute(event);

        verify(notificationRepository).save(entity);
    }
}