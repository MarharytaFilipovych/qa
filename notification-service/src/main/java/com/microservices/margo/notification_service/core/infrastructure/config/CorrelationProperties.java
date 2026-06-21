package com.microservices.margo.notification_service.core.infrastructure.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "correlation")
public record CorrelationProperties(
        @NotBlank String header,
        @NotBlank String key
) { }
