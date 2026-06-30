package com.microservices.margo.gateway.filter;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MediaType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;

@DisplayName("CorrelationIdFilter tests")
class CorrelationIdFilterTest {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(filter, "correlationIdHeader", CORRELATION_ID_HEADER);
        ReflectionTestUtils.setField(filter, "mdcKey", MDC_KEY);
    }

    @Test
    @DisplayName("Filter should extract correlation id from the request header if it exists, " +
            "put it to MDC and response header, forward it on the mutated request, and clean MDC")
    void doFilterInternal_shouldExtractCorrelationIdFromHeaderIfExists()
            throws ServletException, IOException {
        // Arrange
        String correlationId = UUID.randomUUID().toString();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CORRELATION_ID_HEADER, correlationId);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        try (MockedStatic<MDC> mdcMock = mockStatic(MDC.class)) {
            // Act
            filter.doFilterInternal(request, response, chain);

            // Assert
            mdcMock.verify(() -> MDC.put(MDC_KEY, correlationId));
            mdcMock.verify(() -> MDC.remove(MDC_KEY));
        }

        assertThat(response.getHeader(CORRELATION_ID_HEADER)).isEqualTo(correlationId);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\n", "\t"})
    @DisplayName("Filter should generate correlation id when header is absent/blank, " +
            "put it to MDC and response header, and clean MDC")
    void doFilterInternal_shouldGenerateCorrelationIdIfDoesNotExist(String correlationId)
            throws ServletException, IOException {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (correlationId != null) {
            request.addHeader(CORRELATION_ID_HEADER, correlationId);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        try (MockedStatic<MDC> mdcMock = mockStatic(MDC.class)) {
            // Act
            filter.doFilterInternal(request, response, chain);

            // Assert
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            mdcMock.verify(() -> MDC.put(eq(MDC_KEY), captor.capture()));
            mdcMock.verify(() -> MDC.remove(MDC_KEY));

            assertThat(captor.getValue()).satisfies(value ->
                    assertThat(UUID.fromString(value)).isNotNull());
        }

        assertThat(response.getHeader(CORRELATION_ID_HEADER)).isNotNull();
    }

    @Test
    @DisplayName("Mutated request wrapper should override getHeader to return the injected correlation id")
    void doFilterInternal_mutatedRequestShouldReturnCorrelationIdFromGetHeader()
            throws ServletException, IOException {
        // Arrange
        String correlationId = UUID.randomUUID().toString();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CORRELATION_ID_HEADER, correlationId);
        MockHttpServletResponse response = new MockHttpServletResponse();

        final HttpServletRequest[] capturedRequest = new HttpServletRequest[1];
        MockFilterChain chain = new MockFilterChain() {
            @Override
            public void doFilter(@NonNull ServletRequest req, @NonNull ServletResponse res) {
                capturedRequest[0] = (HttpServletRequest) req;
            }
        };

        try (MockedStatic<MDC> ignored = mockStatic(MDC.class)) {
            // Act
            filter.doFilterInternal(request, response, chain);
        }

        // Assert
        assertThat(capturedRequest[0].getHeader(CORRELATION_ID_HEADER)).isEqualTo(correlationId);
        assertThat(Collections.list(capturedRequest[0].getHeaders(CORRELATION_ID_HEADER)))
                .containsExactly(correlationId);
    }
    @Test
    @DisplayName("Mutated request wrapper should delegate getHeader to super for unrelated headers")
    void doFilterInternal_mutatedRequestShouldDelegateGetHeaderForOtherHeaders()
            throws ServletException, IOException {
        // Arrange
        String correlationId = UUID.randomUUID().toString();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CORRELATION_ID_HEADER, correlationId);
        request.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        MockHttpServletResponse response = new MockHttpServletResponse();

        final HttpServletRequest[] capturedRequest = new HttpServletRequest[1];
        MockFilterChain chain = new MockFilterChain() {
            @Override
            public void doFilter(@NonNull ServletRequest req, @NonNull ServletResponse res) {
                capturedRequest[0] = (HttpServletRequest) req;
            }
        };

        try (MockedStatic<MDC> ignored = mockStatic(MDC.class)) {
            // Act
            filter.doFilterInternal(request, response, chain);
        }

        // Assert
        assertThat(capturedRequest[0].getHeader(HttpHeaders.ACCEPT)).isEqualTo( MediaType.APPLICATION_JSON.toString());
        assertThat(Collections.list(capturedRequest[0].getHeaders(HttpHeaders.ACCEPT)))
                .containsExactly(MediaType.APPLICATION_JSON.toString());
    }

    @Test
    void doFilterInternal_shouldRemoveMdcEvenWhenChainThrows() throws IOException {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain() {
            @Override
            public void doFilter(@NonNull ServletRequest req, @NonNull ServletResponse res)
                    throws ServletException {
                throw new ServletException("chain error");
            }
        };

        try (MockedStatic<MDC> mdcMock = mockStatic(MDC.class)) {
            // Act
            try {
                filter.doFilterInternal(request, response, chain);
            } catch (ServletException ignored) {
                // ignore
            }

            // Assert
            mdcMock.verify(() -> MDC.remove(MDC_KEY));
        }
    }
}