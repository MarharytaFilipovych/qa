package com.microservices.margo.order_service.core.application.request;

import com.microservices.margo.order_service.core.domain.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(@NotNull OrderStatus newStatus) {}
