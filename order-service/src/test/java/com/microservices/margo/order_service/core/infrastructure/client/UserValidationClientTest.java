package com.microservices.margo.order_service.core.infrastructure.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.net.SocketTimeoutException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("UserValidationClient tests")
class UserValidationClientTest {

    private static final String USERS_URL = "http://user-service/api/users/";
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock private RestClient restClient;

    private UserValidationClient client;

    @BeforeEach
    void setUp() {
        client = new UserValidationClient(restClient);
        ReflectionTestUtils.setField(client, "usersUrl", USERS_URL);
    }

    @Test
    void validateUserExists_shouldCallRestClientSuccessfully() {
        // Arrange
        RestClient.ResponseSpec responseSpec = mockRequest();

        doReturn(responseSpec).when(responseSpec).onStatus(any(), any());
        doReturn(null).when(responseSpec).toBodilessEntity();

        // Act & Assert
        assertThatNoException().isThrownBy(() -> client.validateUserExists(USER_ID));
    }

    @Test
    void validateUserExists_shouldThrowWhenUserNotFound() {
        // Arrange
        RestClient.ResponseSpec responseSpec = mockRequest();

        doAnswer(inv -> {
            RestClient.ResponseSpec.ErrorHandler handler = inv.getArgument(1);
            handler.handle(mock(HttpRequest.class), mock(ClientHttpResponse.class));
            return responseSpec;
        }).when(responseSpec).onStatus(any(), any());

        // Act & Assert
        assertThatThrownBy(() -> client.validateUserExists(USER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User not found: " + USER_ID);
    }

    @Test
    void fallback_shouldThrow503WhenCauseIsNotSocketTimeout() {
        // Arrange
        ResourceAccessException e = new ResourceAccessException("connection refused");

        // Act & Assert
        assertThatThrownBy(() -> client.fallback(e, USER_ID))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Test
    void fallback_shouldThrow504WhenCauseIsSocketTimeout() {
        // Arrange
        ResourceAccessException e = new ResourceAccessException("timeout", new SocketTimeoutException());

        // Act & Assert
        assertThatThrownBy(() -> client.fallback(e, USER_ID))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.GATEWAY_TIMEOUT));
    }

    @Test
    void fallback_shouldRethrow400WhenIllegalArgumentException() {
        // Arrange
        String error = "User was not found!";
        IllegalArgumentException e = new IllegalArgumentException(error);

        // Act & Assert
        assertThatThrownBy(() -> client.fallback(e, USER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(error);
    }

    private RestClient.ResponseSpec mockRequest() {
        RestClient.RequestHeadersUriSpec<?> uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec<?> headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        doReturn(uriSpec).when(restClient).get();
        doReturn(headersSpec).when(uriSpec).uri(USERS_URL + USER_ID);
        doReturn(responseSpec).when(headersSpec).retrieve();
        return responseSpec;
    }
}