package com.microservices.margo.notification_service.core.infrastructure.repository;

import com.microservices.margo.notification_service.core.infrastructure.entity.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

@SuppressWarnings("NullableProblems")
public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {}

