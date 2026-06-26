package com.microservices.margo.order_service.api.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    @Value("${correlation.header}")
    private String correlationIdHeader;

    @Value("${correlation.key}")
    private String mdcKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String correlationId = request.getHeader(correlationIdHeader);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        MDC.put(mdcKey, correlationId);
        response.setHeader(correlationIdHeader, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(mdcKey);
        }
    }
}