package com.microservices.margo.order_service.api.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNSUPPORTED_MEDIA_TYPE;

@WebMvcTest(GlobalExceptionHandler.class)
@DisplayName("GlobalExceptionHandler tests")
class GlobalExceptionHandlerTest {
    private static final String EXCEPTION = "Oh nooooo!";

    @Autowired
    private GlobalExceptionHandler handler;

    @Test
    void handleGeneralException_shouldReturn500() {
        // Arrange
        Exception exception = new RuntimeException(EXCEPTION);

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleGeneralException(exception);

        // Assert
        assertErrorResponse(response, "Unexpected exception occurred", INTERNAL_SERVER_ERROR);
    }

    @Test
    void handleHttpRequestMethodNotSupportedException_shouldReturn405() {
        // Arrange
        HttpRequestMethodNotSupportedException exception =
                new HttpRequestMethodNotSupportedException("DELETE");

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleHttpRequestMethodNotSupportedException(exception);

        // Assert
        assertErrorResponse(response, exception.getMessage(), METHOD_NOT_ALLOWED);
    }

    @Test
    void handleHttpMediaTypeNotSupportedException_shouldReturn415() {
        // Arrange
        HttpMediaTypeNotSupportedException exception = new HttpMediaTypeNotSupportedException("beauty");

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleHttpMediaTypeNotSupportedException(exception);

        // Assert
        assertErrorResponse(response, exception.getMessage(), UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    void handleJsonParseError_shouldReturn400() {
        // Arrange
        HttpInputMessage inputMessage = Mockito.mock(HttpInputMessage.class);
        HttpMessageNotReadableException exception = new HttpMessageNotReadableException(EXCEPTION, inputMessage);

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleJsonParseError(exception);

        // Assert
        assertErrorResponse(response, "Invalid JSON format in request body", BAD_REQUEST);
    }

    @Test
    void handleValidationException_shouldReturn400WithDescription() throws Exception {
        // Arrange
        Map<String, String> messages = Map.of("age", "Age must be provided.",
                "address", "Address must be in the correct format.",
                "money", "You must not be poor.");
        String expectedErrorMessage = messages.entrySet().stream().map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining("\n"));

        MethodArgumentNotValidException exception = buildMethodArgumentNotValidException(messages);

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleValidationException(exception);

        // Assert
        assertErrorResponse(response, expectedErrorMessage, BAD_REQUEST);
    }

    @Test
    void handleConstraintViolationException_shouldReturn400() {
        // Arrange
        List<String> messages = List.of("must be greater than 0", "must not be blank");
        ConstraintViolationException exception = buildConstraintViolationException(messages);
        String expectedErrorMessage = String.join("\n", messages);

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleConstraintViolationException(exception);

        // Assert
        assertErrorResponse(response, expectedErrorMessage, BAD_REQUEST);
    }

    @Test
    void handleErrorResponses_whenIllegalArgument_shouldReturn400() {
        // Arrange
        IllegalArgumentException exception = new IllegalArgumentException(EXCEPTION);

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleErrorResponses(exception);

        // Assert
        assertErrorResponse(response, EXCEPTION, BAD_REQUEST);
    }

    @Test
    void handleErrorResponses_whenIllegalState_shouldReturn400() {
        // Arrange
        IllegalStateException exception = new IllegalStateException(EXCEPTION);

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleErrorResponses(exception);

        // Assert
        assertErrorResponse(response, EXCEPTION, BAD_REQUEST);
    }

    @Test
    void handleNotFoundException_withExplicitlyProvidedMessage_shouldReturn404() {
        // Arrange
        EntityNotFoundException exception = new EntityNotFoundException(EXCEPTION);

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleNotFoundException(exception);

        // Assert
        assertErrorResponse(response, EXCEPTION, NOT_FOUND);
    }

    @Test
    void handleNotFoundException_nullMessage_shouldReturnErrorMessageFallbackAnd404() {
        // Arrange
        EntityNotFoundException exception = new EntityNotFoundException();

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleNotFoundException(exception);

        // Assert
        assertErrorResponse(response, "Not found!", NOT_FOUND);
    }

    @Test
    void handleMethodArgumentTypeMismatch_shouldReturn400() {
        // Arrange
        String parameter = "id";
        String value = "beautiful value";
        MethodParameter methodParameter = mock(MethodParameter.class);
        MethodArgumentTypeMismatchException exception =
                new MethodArgumentTypeMismatchException(value, String.class, parameter, methodParameter, new RuntimeException());


        // Act
        ResponseEntity<ErrorResponse> response = handler.handleMethodArgumentTypeMismatch(exception);

        // Assert
        assertErrorResponse(response, "Invalid value for parameter '%s': %s".formatted(parameter, value), BAD_REQUEST);
    }

    @Test
    void handleDataIntegrityViolation_shouldReturn409() {
        // Arrange
        DataIntegrityViolationException exception = new DataIntegrityViolationException(EXCEPTION);
        // Act
        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolation(exception);

        // Assert
        assertErrorResponse(response, "Failed to save entity because some rules where neglected.", CONFLICT);
    }

    @ParameterizedTest
    @ValueSource(ints = {404, 400, 403, 409, 406})
    void handleResponseStatus_shouldReturnRelevantStatusCode(int statusCode) {
        // Arrange
        ResponseStatusException exception = new ResponseStatusException(HttpStatusCode.valueOf(statusCode), EXCEPTION);

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleResponseStatus(exception);

        // Assert
        assertErrorResponse(response, EXCEPTION, HttpStatus.valueOf(statusCode));
    }

    @Test
    void handleNoHandlerFound_shouldReturn404() {
        // Arrange
        String method = HttpMethod.GET.name();
        String path = "/beauty";
        NoHandlerFoundException exception = new NoHandlerFoundException(
                method, path, new HttpHeaders());

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleNoHandlerFound(exception);

        // Assert
        assertErrorResponse(response, "No endpoint %s %s".formatted(method, path), NOT_FOUND);
    }

    private MethodArgumentNotValidException buildMethodArgumentNotValidException(
            Map<String,String> messages) throws Exception {

        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Object(), "target");

        messages.forEach((key, value) -> bindingResult.addError(new FieldError("target", key, value)));

        return new MethodArgumentNotValidException(
                new MethodParameter(Object.class.getDeclaredMethod("toString"), -1),
                bindingResult);
    }

    private ConstraintViolationException buildConstraintViolationException(List<String> messages) {
        Set<ConstraintViolation<?>> violations = messages.stream()
                .map(message -> {
                    ConstraintViolation<?> cv = mock(ConstraintViolation.class);
                    when(cv.getMessage()).thenReturn(message);
                    return cv;
                })
                .collect(Collectors.toSet());
        return new ConstraintViolationException(violations);
    }

    private void assertErrorResponse(ResponseEntity<ErrorResponse> response,
                                     String expectedMessage, HttpStatus expectedStatus) {
        assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
        assertNotNull(response.getBody());
        String body = response.getBody().message();
        assertThat(body).isEqualTo(expectedMessage);
    }
}