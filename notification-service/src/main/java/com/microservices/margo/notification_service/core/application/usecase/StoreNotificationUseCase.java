package com.microservices.margo.notification_service.core.application.usecase;

import com.microservices.margo.notification_service.core.application.mapper.NotificationMapper;
import com.microservices.margo.notification_service.core.application.event.OrderCreatedEvent;
import com.microservices.margo.notification_service.core.infrastructure.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoreNotificationUseCase {
    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    public void execute(OrderCreatedEvent event) {
        try {
            notificationRepository.save(notificationMapper.toEntity(event));
            log.info("Stored notification for eventId={}", event.eventId());
        } catch (DataIntegrityViolationException e) {
            log.warn("Duplicate eventId={}, ignoring", event.eventId());
            log.error("Error message: {}", e.getMessage());
        }
    }
}