package com.microservices.margo.order_service.core.infrastructure.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.net.SocketTimeoutException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserValidationClientTest {

    @Mock RestClient restClient;
    @Mock RestClient.RequestHeadersUriSpec<?> uriSpec;
    @Mock RestClient.RequestHeadersSpec<?> headersSpec;
    @Mock RestClient.ResponseSpec responseSpec;

    @InjectMocks UserValidationClient client;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(client, "usersUrl", "http://user-service/api/users/");
        doReturn(uriSpec).when(restClient).get();
        doReturn(headersSpec).when(uriSpec).uri(anyString());
        doReturn(responseSpec).when(headersSpec).retrieve();
    }

    @Test
    void validateUserExists_succeedsWhenUserFound() {
        UUID userId = UUID.randomUUID();
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(null);

        // should not throw
        client.validateUserExists(userId);
    }

    @Test
    void fallback_throws503_whenResourceAccessException() {
        UUID userId = UUID.randomUUID();
        ResourceAccessException ex = new ResourceAccessException("connect refused");

        assertThatThrownBy(() -> client.fallback(ex, userId))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void fallback_throws504_whenCauseIsSocketTimeout() {
        UUID userId = UUID.randomUUID();
        ResourceAccessException ex = new ResourceAccessException("timeout",
                new SocketTimeoutException("timed out"));

        assertThatThrownBy(() -> client.fallback(ex, userId))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
    }
}