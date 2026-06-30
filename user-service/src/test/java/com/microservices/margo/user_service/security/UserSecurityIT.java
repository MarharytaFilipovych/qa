package com.microservices.margo.user_service.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservices.margo.user_service.core.application.request.CreateUserRequest;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.UnsupportedEncodingException;

import static com.microservices.margo.user_service.data.Constants.CORRELATION_ID;
import static com.microservices.margo.user_service.data.Constants.CORRELATION_ID_HEADER;
import static com.microservices.margo.user_service.data.Constants.MESSAGE_IN_PAYLOAD;
import static com.microservices.margo.user_service.data.Constants.SLASH;
import static com.microservices.margo.user_service.data.Constants.USER_PATH;
import static com.microservices.margo.user_service.data.UserData.USER_ID;
import static com.microservices.margo.user_service.data.UserData.createUserRequest;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("UserService Security Tests")
class UserSecurityIT {

    private static final String UUID_PATTERN =
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String json(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    private String getId(MvcResult result) throws UnsupportedEncodingException, JsonProcessingException {
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asText();
    }

    @Test
    @SneakyThrows
    @DisplayName("Should propagate existing X-Correlation-Id through the full filter chain")
    void shouldPropagateExistingCorrelationId() {
        // Act & Assert
        mockMvc.perform(get(USER_PATH + SLASH +  USER_ID)
                        .header(CORRELATION_ID_HEADER, CORRELATION_ID))
                .andExpect(header().string(CORRELATION_ID_HEADER, CORRELATION_ID));
    }

    @Test
    @SneakyThrows
    @DisplayName("Should generate a valid UUID X-Correlation-Id when none is provided")
    void shouldGenerateCorrelationIdWhenAbsent() {
        // Act & Assert
        mockMvc.perform(get(USER_PATH + SLASH + USER_ID))
                .andExpect(header().string(CORRELATION_ID_HEADER, matchesPattern(UUID_PATTERN)));
    }

    @Test
    @SneakyThrows
    @DisplayName("Should generate a fresh UUID correlation ID when a blank value is sent")
    void shouldReplaceBlankCorrelationId() {
        // Act & Assert
        mockMvc.perform(get(USER_PATH + SLASH +  USER_ID)
                        .header(CORRELATION_ID_HEADER, "   "))
                .andExpect(header().string(CORRELATION_ID_HEADER, matchesPattern(UUID_PATTERN)));
    }

    @Test
    @SneakyThrows
    @DisplayName("X-Correlation-Id must be present even on validation error responses")
    void shouldReturnCorrelationIdOnErrorResponse()  {
        // Act & Assert
        mockMvc.perform(post(USER_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().exists(CORRELATION_ID_HEADER));
    }

    @Test
    @SneakyThrows
    @DisplayName("X-Correlation-Id must be present on 404 error responses")
    void shouldReturnCorrelationIdOn404Response()  {
        // Act & Assert
        mockMvc.perform(get(USER_PATH + SLASH +  USER_ID))
                .andExpect(status().isNotFound())
                .andExpect(header().exists(CORRELATION_ID_HEADER));
    }
    @ParameterizedTest
    @ValueSource(strings = {
            "'; DROP TABLE users; --",
            "' OR '1'='1",
            "admin'--",
            "1; SELECT * FROM users",
            "' UNION SELECT null, null, null--"
    })
    @SneakyThrows
    @DisplayName("SQL-injection payload in name is stored as a literal string (parameterized queries protect the DB)")
    void sqlInjectionInNameIsStoredSafely(String maliciousInput) {
        // Arrange
        CreateUserRequest request = createUserRequest().toBuilder()
                .name(maliciousInput)
                .email(Math.abs(maliciousInput.hashCode()) + "@example.com")
                .build();

        // Act
        MvcResult result = mockMvc.perform(post(USER_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isCreated())
                .andReturn();

        // Assert
        mockMvc.perform(get(USER_PATH + SLASH + getId(result)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(maliciousInput));
    }

    @Test
    @SneakyThrows
    @DisplayName("SQL-injection in one user's name does not corrupt another user's data")
    void sqlInjectionDoesNotCorruptOtherRows() {
        // Arrange
        CreateUserRequest victim = createUserRequest().toBuilder()
                .email("victim-sql@example.com")
                .build();

        MvcResult victimResult = mockMvc.perform(post(USER_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(victim)))
                .andExpect(status().isCreated())
                .andReturn();


        CreateUserRequest attacker = createUserRequest().toBuilder()
                .name("' UNION SELECT id, email, email, email, now(), now() FROM users--")
                .email("attacker-sql@example.com")
                .build();
        mockMvc.perform(post(USER_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(attacker)))
                .andExpect(status().isCreated());

        // Act & Assert
        mockMvc.perform(get(USER_PATH + SLASH + getId(victimResult)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(victim.email()))
                .andExpect(jsonPath("$.name").value(victim.name()));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "'; DROP TABLE users; --@x.com",
            "' OR 1=1--@x.com",
            "notanemail"
    })
    @SneakyThrows
    @DisplayName("SQL-injection strings that are also malformed emails are rejected with 400")
    void sqlInjectionThatIsAlsoInvalidEmailIsRejected(String maliciousEmail) {
        // Act & Assert
        mockMvc.perform(post(USER_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(createUserRequest().toBuilder().email(maliciousEmail).build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "<script>alert('xss')</script>@evil.com",
            "xss@<script>.com"
    })
    @SneakyThrows
    @DisplayName("XSS payloads that are also malformed emails are rejected with 400")
    void xssEmailsThatAreInvalidAreRejected(String xssEmail) {
        // Act & Assert
        mockMvc.perform(post(USER_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(createUserRequest().toBuilder().email(xssEmail).build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(MESSAGE_IN_PAYLOAD).isNotEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "<script>alert('xss')</script>",
            "<img src=x onerror=alert(1)>",
            "javascript:alert(document.cookie)"
    })
    @SneakyThrows
    @DisplayName("Error responses must never reflect raw XSS payloads back to the caller")
    void errorResponsesMustNotReflectXssPayloads(String xssPayload)  {
        // Act & Assert
        mockMvc.perform(post(USER_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(createUserRequest().toBuilder()
                                .name(xssPayload)
                                .email("not-a-valid-email")
                                .build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(MESSAGE_IN_PAYLOAD).value("email: must be a well-formed email address"))
                .andExpect(jsonPath(MESSAGE_IN_PAYLOAD).value(not(containsString(xssPayload))));
    }

    @Test
    @SneakyThrows
    @DisplayName("Should return 409 Conflict when the same email is registered twice")
    void shouldReturn409OnDuplicateEmail() {
        // Arrange
        String email = "unique-" + System.nanoTime() + "@example.com";
        CreateUserRequest request = createUserRequest().toBuilder()
                .email(email)
                .build();

        // Act & Assert
        mockMvc.perform(post(USER_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post(USER_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath(MESSAGE_IN_PAYLOAD).value("User with email %s already exists!".formatted(email)));
    }

    @Test
    @SneakyThrows
    @DisplayName("Should return 415 Unsupported Media Type when Content-Type is text/plain")
    void shouldReturn415ForWrongContentType() {
        // Act & Assert
        mockMvc.perform(post(USER_PATH)
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(json(createUserRequest())))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath(MESSAGE_IN_PAYLOAD).value("Content-Type 'text/plain;charset=UTF-8' is not supported"));
    }

    @ParameterizedTest
    @SneakyThrows
    @ValueSource(strings = {"{\"name\": \"Alice\"", "not-json", })
    @DisplayName("Should return 400 with a safe error message for invalid JSON")
    void shouldReturn400ForTruncatedJson(String requestBody) {
        // Act & Assert
        mockMvc.perform(post(USER_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(MESSAGE_IN_PAYLOAD).value("Invalid JSON format in request body"));
    }

    @Test
    @SneakyThrows
    @DisplayName("Non-UUID path segment should return a structured JSON error, not a Whitelabel page")
    void nonUuidPathParamShouldReturnStructuredError() {
        // Act & Assert
        mockMvc.perform(get(USER_PATH + "/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath(MESSAGE_IN_PAYLOAD).value("Invalid value for parameter 'id': not-a-uuid"));
    }

    @Test
    @SneakyThrows
    @DisplayName("Should return 404 for unknown routes with a structured JSON body")
    void shouldReturn404ForUnknownRoute() {
        // Act & Assert
        mockMvc.perform(get("/unknown-route"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath(MESSAGE_IN_PAYLOAD).value("No endpoint GET /unknown-route"));
    }

    @Test
    @SneakyThrows
    @DisplayName("Should return 405 for unsupported HTTP methods with a structured JSON body")
    void shouldReturn405ForUnsupportedMethod() {
        // Act & Assert
        mockMvc.perform(delete(USER_PATH))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath(MESSAGE_IN_PAYLOAD).value("Request method 'DELETE' is not supported"));
    }
}