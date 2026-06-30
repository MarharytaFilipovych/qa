package com.microservices.margo.user_service.data;

import java.util.UUID;

public final class Constants {
    private Constants() { }

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";
    public static final String CORRELATION_ID = UUID.randomUUID().toString();
    public static final String USER_PATH = "/users";
    public static final String SLASH = "/";
    public static final String MESSAGE_IN_PAYLOAD = "$.message";
}
