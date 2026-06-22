package com.microservices.margo.order_service.api.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CorrelationIdFilterTest {

    @Mock HttpServletRequest request;
    @Mock HttpServletResponse response;
    @Mock FilterChain chain;

    @InjectMocks CorrelationIdFilter filter;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(filter, "correlationIdHeader", "X-Correlation-Id");
        ReflectionTestUtils.setField(filter, "mdcKey", "correlationId");
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void usesExistingCorrelationId_whenHeaderPresent() throws Exception {
        when(request.getHeader("X-Correlation-Id")).thenReturn("existing-id");

        filter.doFilterInternal(request, response, chain);

        verify(response).setHeader("X-Correlation-Id", "existing-id");
        verify(chain).doFilter(request, response);
    }

    @Test
    void generatesNewCorrelationId_whenHeaderMissing() throws Exception {
        when(request.getHeader("X-Correlation-Id")).thenReturn(null);

        filter.doFilterInternal(request, response, chain);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(response).setHeader(eq("X-Correlation-Id"), captor.capture());
        assertThat(captor.getValue()).isNotBlank();
        verify(chain).doFilter(request, response);
    }

    @Test
    void generatesNewCorrelationId_whenHeaderBlank() throws Exception {
        when(request.getHeader("X-Correlation-Id")).thenReturn("   ");

        filter.doFilterInternal(request, response, chain);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(response).setHeader(eq("X-Correlation-Id"), captor.capture());
        assertThat(captor.getValue()).isNotBlank();
    }

    @Test
    void clearsMdc_afterFilterChain() throws Exception {
        when(request.getHeader("X-Correlation-Id")).thenReturn("id-123");

        filter.doFilterInternal(request, response, chain);

        assertThat(MDC.get("correlationId")).isNull();
    }

    @Test
    void clearsMdc_evenWhenChainThrows() throws Exception {
        when(request.getHeader("X-Correlation-Id")).thenReturn("id-123");
        doThrow(new RuntimeException("chain error")).when(chain).doFilter(any(), any());

        try {
            filter.doFilterInternal(request, response, chain);
        } catch (RuntimeException ignored) {}

        assertThat(MDC.get("correlationId")).isNull();
    }
}