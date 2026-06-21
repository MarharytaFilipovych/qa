package com.microservices.margo.workflow_service.core.infrastructure.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@AllArgsConstructor
@ConfigurationProperties(prefix = "order.service")
public class OrderServiceProperties {
    @Valid
    private Url url;

    @Valid
    private Params params;

    public record Url(
        @NotBlank
        String base,

        @NotBlank
        String createOrder,

        @NotBlank
        String changeStatus
    ){}

    public record Params(
        @NotBlank
        String ownerUserId,

        @NotBlank
        String itemName,

        @NotBlank
        String quantity,

        @NotBlank
        String price,

        @NotBlank
        String newStatus,

        @NotBlank
        String id
    ){}
}
