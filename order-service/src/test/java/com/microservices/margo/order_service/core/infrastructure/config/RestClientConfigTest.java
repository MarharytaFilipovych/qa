package com.microservices.margo.order_service.core.infrastructure.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static com.microservices.margo.order_service.data.Constants.CORRELATION_ID;
import static com.microservices.margo.order_service.data.Constants.CORRELATION_ID_HEADER;
import static com.microservices.margo.order_service.data.Constants.MDC_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RestClientConfigTest {

    private final CorrelationProperties correlationProperties = new CorrelationProperties(CORRELATION_ID_HEADER, MDC_KEY);
    private final RestClientConfig config = new RestClientConfig(correlationProperties);

    private HttpHeaders headers;
    private HttpRequest request;
    private ClientHttpRequestExecution execution;
    private MockClientHttpResponse response;

    @BeforeEach
    void setUp() throws IOException{
        headers = new HttpHeaders();
        request = mock(HttpRequest.class);
        when(request.getHeaders()).thenReturn(headers);

        response = new MockClientHttpResponse(new byte[0], HttpStatus.OK);
        execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(any(), any())).thenReturn(response);
    }

    @AfterEach
    void clearMdc() {
        response.close();
        MDC.remove(MDC_KEY);
    }

    private ClientHttpRequestInterceptor interceptor() {
        ReflectionTestUtils.setField(config, "connectTimeout", 3000);
        ReflectionTestUtils.setField(config, "readTimeout", 5000);
        return ReflectionTestUtils.invokeMethod(config, "correlationIdInterceptor");
    }

    @Test
    void correlationIdInterceptor_shouldSetHeaderWhenCorrelationIdPresentInMdc() throws IOException {
        // Arrange
        MDC.put(MDC_KEY, CORRELATION_ID);

        // Act
        try (ClientHttpResponse ignored = interceptor().intercept(request, new byte[0], execution)) {
            // Assert
            assertThat(headers.getFirst(CORRELATION_ID_HEADER)).isEqualTo(CORRELATION_ID);
        }
    }

    @Test
    void correlationIdInterceptor_shouldNotSetHeaderWhenCorrelationIdAbsentFromMdc() throws IOException {
        // Act
        try (ClientHttpResponse ignored = interceptor().intercept(request, new byte[0], execution)) {
            // Assert
            assertThat(headers.getFirst(CORRELATION_ID_HEADER)).isNull();
        }
    }
}