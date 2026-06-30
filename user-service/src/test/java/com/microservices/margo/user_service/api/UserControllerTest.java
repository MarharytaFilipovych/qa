package com.microservices.margo.user_service.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservices.margo.user_service.core.application.exception.UserAlreadyExistsException;
import com.microservices.margo.user_service.core.application.request.CreateUserRequest;
import com.microservices.margo.user_service.core.application.usecase.CreateUserUseCase;
import com.microservices.margo.user_service.core.application.usecase.GetUserUseCase;
import com.microservices.margo.user_service.core.domain.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

import static com.microservices.margo.user_service.data.Constants.MESSAGE_IN_PAYLOAD;
import static com.microservices.margo.user_service.data.Constants.SLASH;
import static com.microservices.margo.user_service.data.Constants.USER_PATH;
import static com.microservices.margo.user_service.data.UserData.TOO_LONG_VALUE;
import static com.microservices.margo.user_service.data.UserData.createUserRequest;
import static com.microservices.margo.user_service.data.UserData.getUser;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("UserController tests")
class UserControllerTest {
    private static final User USER = getUser();
    private static final CreateUserRequest CREATE_USER_REQUEST = createUserRequest();

    @MockitoBean
    private CreateUserUseCase createUserUseCase;

    @MockitoBean
    private GetUserUseCase getUserUseCase;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @SneakyThrows
    void create_shouldReturnCreatedUserWithLocation() {
        // Arrange
        when(createUserUseCase.execute(CREATE_USER_REQUEST)).thenReturn(USER);

        // Act & Assert
        andExpectUser(mockMvc.perform(post(USER_PATH).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CREATE_USER_REQUEST)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString(USER_PATH + SLASH + USER.id()))));
    }

    @ParameterizedTest
    @SneakyThrows
    @MethodSource("com.microservices.margo.user_service.data.UserData#underageBirthDates")
    void create_shouldReturnBadRequestWhenBirthDateIsInvalid(LocalDate birthDate) {
        // Arrange
        CreateUserRequest createUserRequest = CREATE_USER_REQUEST.toBuilder().birthDate(birthDate).build();

        // Act & Assert
        assertBadRequest(createUserRequest, "birthDate: User must be at least 14 years old");
    }

    @ParameterizedTest
    @SneakyThrows
    @NullAndEmptySource
    void create_shouldReturnBadRequestWhenNameIsNotProvided(String name) {
        // Arrange
        CreateUserRequest createUserRequest = CREATE_USER_REQUEST.toBuilder().name(name).build();

        // Act & Assert
        assertBadRequest(createUserRequest, "name: Name must not be blank");
    }

    @Test
    @SneakyThrows
    void create_shouldReturnBadRequestWhenNameIsTooLong() {
        // Arrange
        CreateUserRequest createUserRequest = CREATE_USER_REQUEST.toBuilder().name(TOO_LONG_VALUE).build();

        // Act & Assert
        assertBadRequest(createUserRequest, "name: Name can at most contain 255 symbols");
    }

    @ParameterizedTest
    @SneakyThrows
    @NullAndEmptySource
    void create_shouldReturnBadRequestWhenSurnameIsNotProvided(String surname) {
        // Arrange
        CreateUserRequest createUserRequest = CREATE_USER_REQUEST.toBuilder().surname(surname).build();

        // Act & Assert
        assertBadRequest(createUserRequest, "surname: Surname must not be blank");
    }

    @Test
    @SneakyThrows
    void create_shouldReturnBadRequestWhenSurnameIsTooLong() {
        // Arrange
        CreateUserRequest createUserRequest = CREATE_USER_REQUEST.toBuilder().surname(TOO_LONG_VALUE).build();

        // Act & Assert
        assertBadRequest(createUserRequest, "surname: Surname can at most contain 255 symbols");
    }

    @ParameterizedTest
    @SneakyThrows
    @NullAndEmptySource
    void create_shouldReturnBadRequestWhenEmailIsAbsent(String email) {
        // Arrange
        CreateUserRequest createUserRequest = CREATE_USER_REQUEST.toBuilder().email(email).build();

        // Act & Assert
        assertBadRequest(createUserRequest, "email: Email must be specified");
    }

    @ParameterizedTest
    @SneakyThrows
    @MethodSource("com.microservices.margo.user_service.data.UserData#incorrectEmails")
    void create_shouldReturnBadRequestWhenEmailIsInvalid(String email) {
        // Arrange
        CreateUserRequest createUserRequest = CREATE_USER_REQUEST.toBuilder().email(email).build();

        // Act & Assert
        assertBadRequest(createUserRequest, "email: must be a well-formed email address");
    }

    @ParameterizedTest
    @SneakyThrows
    @MethodSource("com.microservices.margo.user_service.data.UserData#incorrectPhones")
    void create_shouldReturnBadRequestWhenPhoneIsInvalid(String phone) {
        // Arrange
        CreateUserRequest createUserRequest = CREATE_USER_REQUEST.toBuilder().phone(phone).build();

        // Act & Assert
        assertBadRequest(createUserRequest, "phone: Phone number must be 7–20 digits and may include +, spaces, dashes, or parentheses");
    }

    @Test
    @SneakyThrows
    void create_shouldPropagateUserExistExceptionsFromServiceAs409() {
        // Arrange
        String errorMessage = "User exists!";
        when(createUserUseCase.execute(CREATE_USER_REQUEST)).thenThrow(new UserAlreadyExistsException(errorMessage));

        // Act & Assert
        mockMvc.perform(post(USER_PATH).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CREATE_USER_REQUEST)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath(MESSAGE_IN_PAYLOAD).value(errorMessage));
    }

    @Test
    @SneakyThrows
    void getById_shouldReturnUser() {
        // Arrange
        when(getUserUseCase.execute(USER.id())).thenReturn(USER);

        // Act & Assert
        andExpectUser(mockMvc.perform(get(USER_PATH + SLASH + USER.id()))
                .andExpect(status().isOk()));

    }

    @Test
    @SneakyThrows
    void getById_shouldPropagateNotFoundAs404() {
        // Arrange
        String errorMessage = "Not found!";
        when(getUserUseCase.execute(USER.id())).thenThrow(new EntityNotFoundException(errorMessage));

        // Act & Assert
        mockMvc.perform(get(USER_PATH + SLASH + USER.id()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath(MESSAGE_IN_PAYLOAD).value(errorMessage));
    }

    private void assertBadRequest(CreateUserRequest createUserRequest, String expectedErrorMessage) throws Exception {
        mockMvc.perform(post(USER_PATH).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createUserRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(MESSAGE_IN_PAYLOAD).value(expectedErrorMessage));
        verifyNoInteractions(createUserUseCase);
    }

    private void andExpectUser(ResultActions resultActions) throws Exception {
        resultActions
                .andExpect(jsonPath("$.id").value(USER.id().toString()))
                .andExpect(jsonPath("$.name").value(USER.name()))
                .andExpect(jsonPath("$.surname").value(USER.surname()))
                .andExpect(jsonPath("$.birthDate").value(USER.birthDate().toString()))
                .andExpect(jsonPath("$.createdAt").value(USER.createdAt().toString()))
                .andExpect(jsonPath("$.email").value(USER.email()))
                .andExpect(jsonPath("$.phone").value(USER.phone()));
    }
}