package com.microservices.margo.order_service.core.infrastructure.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.web.client.RestClient;

import java.lang.reflect.Method;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RestClientConfigTest {

    private final CorrelationProperties props = new CorrelationProperties("X-Correlation-Id", "correlationId");
    private final RestClientConfig config = new RestClientConfig(props);

    @BeforeEach @AfterEach
    void clearMdc() { MDC.clear(); }

    /** Reflectively extracts the private interceptor so we can test it in isolation. */
    private ClientHttpRequestInterceptor extractInterceptor() throws Exception {
        Method m = RestClientConfig.class.getDeclaredMethod("correlationIdInterceptor");
        m.setAccessible(true);
        return (ClientHttpRequestInterceptor) m.invoke(config);
    }

    @Test
    void interceptor_setsCorrelationHeader_whenMdcPopulated() throws Exception {
        MDC.put("correlationId", "test-corr-id");

        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, URI.create("/test"));
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(any(), any())).thenReturn(new MockClientHttpResponse(new byte[0], HttpStatus.OK));

        extractInterceptor().intercept(request, new byte[0], execution);

        assertThat(request.getHeaders().getFirst("X-Correlation-Id")).isEqualTo("test-corr-id");
    }

    @Test
    void interceptor_doesNotSetHeader_whenMdcEmpty() throws Exception {
        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, URI.create("/test"));
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(any(), any())).thenReturn(new MockClientHttpResponse(new byte[0], HttpStatus.OK));

        extractInterceptor().intercept(request, new byte[0], execution);

        assertThat(request.getHeaders().getFirst("X-Correlation-Id")).isNull();
    }

    @Test
    void restClient_isNotNull() {
        assertThat(config.restClient()).isNotNull();
    }
}