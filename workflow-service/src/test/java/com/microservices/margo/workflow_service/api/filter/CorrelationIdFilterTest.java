package com.microservices.margo.workflow_service.api.filter;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.UUID;

import static com.microservices.margo.workflow_service.data.Constants.CORRELATION_ID;
import static com.microservices.margo.workflow_service.data.Constants.CORRELATION_ID_HEADER;
import static com.microservices.margo.workflow_service.data.Constants.MDC_KEY;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;

@DisplayName("CorrelationIdFilter tests")
class CorrelationIdFilterTest {
    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(filter, "correlationIdHeader", CORRELATION_ID_HEADER);
        ReflectionTestUtils.setField(filter, "mdcKey", MDC_KEY);
    }

    @Test
    @DisplayName("Filter should extract correlation id from the request header if it exists, then put it to MDC " +
            "and the response header, call next chain and finally remove it from MDC")
    void doFilterInternal_shouldExtractCorrelationIdFromHeaderIfExists()
            throws ServletException, IOException {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CORRELATION_ID_HEADER, CORRELATION_ID);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        try (MockedStatic<MDC> mdcMock = mockStatic(MDC.class)) {
            // Act
            filter.doFilterInternal(request, response, chain);

            // Assert
            mdcMock.verify(() -> MDC.put(MDC_KEY, CORRELATION_ID));
            mdcMock.verify(() -> MDC.remove(MDC_KEY));
        }

        assertThat(response.getHeader(CORRELATION_ID_HEADER)).isEqualTo(CORRELATION_ID);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\n", "\t"})
    @DisplayName("Filter should generate correlation id if it does not exist in the request header, then put it to MDC " +
            "and the response header, call next chain and finally remove it from MDC")
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

            assertThat(captor.getValue()).satisfies(value -> {
                UUID parsed = UUID.fromString(value);
                assertThat(parsed).isNotNull();
            });
        }
        String requestHeader = response.getHeader(CORRELATION_ID_HEADER);
        String responseHeader = response.getHeader(CORRELATION_ID_HEADER);
        assertThat(requestHeader).isNotNull();
        assertThat(responseHeader).isNotNull();
        assertThat(responseHeader).isEqualTo(requestHeader);
    }
}