package com.microservices.margo.user_service.core.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record User(
        UUID id,
        String name,
        String surname,
        String phone,
        LocalDate birthDate,
        String email,
        LocalDateTime createdAt
) {}