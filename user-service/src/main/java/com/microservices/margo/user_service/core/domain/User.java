package com.microservices.margo.user_service.core.domain;

import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder(toBuilder = true)
public record User(
        UUID id,
        String name,
        String surname,
        String phone,
        LocalDate birthDate,
        String email,
        LocalDateTime createdAt
) {}