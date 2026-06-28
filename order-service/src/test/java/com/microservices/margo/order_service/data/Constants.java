package com.microservices.margo.order_service.data;

import java.util.UUID;

public final class Constants {
    private Constants () { }

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";
    public static final String CORRELATION_ID = UUID.randomUUID().toString();
}
