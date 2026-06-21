package com.microservices.margo.gateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    @Value("${correlation.header}")
    private String correlationIdHeader;

    @Value("${correlation.key}")
    private String mdcKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String correlationId = request.getHeader(correlationIdHeader);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        MDC.put(mdcKey, correlationId);
        response.setHeader(correlationIdHeader, correlationId);

        final String finalCorrelationId = correlationId;
        HttpServletRequestWrapper mutated = new HttpServletRequestWrapper(request) {
            @Override
            public String getHeader(String name) {
                if (correlationIdHeader.equalsIgnoreCase(name)) return finalCorrelationId;
                return super.getHeader(name);
            }

            @Override
            public Enumeration<String> getHeaders(String name) {
                if (correlationIdHeader.equalsIgnoreCase(name)) {
                    return Collections.enumeration(List.of(finalCorrelationId));
                }
                return super.getHeaders(name);
            }
        };

        try {
            filterChain.doFilter(mutated, response);
        } finally {
            MDC.remove(mdcKey);
        }
    }
}