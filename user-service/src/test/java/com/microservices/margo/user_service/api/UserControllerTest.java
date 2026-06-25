package com.microservices.margo.user_service.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservices.margo.user_service.core.application.exception.UserAlreadyExistsException;
import com.microservices.margo.user_service.core.application.request.CreateUserRequest;
import com.microservices.margo.user_service.core.application.usecase.CreateUserUseCase;
import com.microservices.margo.user_service.core.application.usecase.GetUserUseCase;
import com.microservices.margo.user_service.core.domain.User;
import jakarta.persistence.EntityNotFoundException;
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
    private static final String USER_PATH = "/users";
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
    void create_shouldReturnCreatedUserWithLocation() throws Exception {
        // Arrange
        when(createUserUseCase.execute(CREATE_USER_REQUEST)).thenReturn(USER);

        // Act & Assert
        andExpectUser(mockMvc.perform(post(USER_PATH).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CREATE_USER_REQUEST)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString(USER_PATH + "/" + USER.id()))));
    }

    @ParameterizedTest
    @MethodSource("com.microservices.margo.user_service.data.UserData#underageBirthDates")
    void create_shouldReturnBadRequestWhenBirthDateIsInvalid(LocalDate birthDate) throws Exception {
        // Arrange
        CreateUserRequest createUserRequest = CREATE_USER_REQUEST.toBuilder().birthDate(birthDate).build();

        // Act & Assert
        assertBadRequest(createUserRequest, "birthDate: User must be at least 14 years old");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void create_shouldReturnBadRequestWhenNameIsNotProvided(String name) throws Exception {
        // Arrange
        CreateUserRequest createUserRequest = CREATE_USER_REQUEST.toBuilder().name(name).build();

        // Act & Assert
        assertBadRequest(createUserRequest, "name: Name must not be blank");
    }

    @Test
    void create_shouldReturnBadRequestWhenNameIsTooLong() throws Exception {
        // Arrange
        CreateUserRequest createUserRequest = CREATE_USER_REQUEST.toBuilder().name(TOO_LONG_VALUE).build();

        // Act & Assert
        assertBadRequest(createUserRequest, "name: Name can at most contain 255 symbols");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void create_shouldReturnBadRequestWhenSurnameIsNotProvided(String surname) throws Exception {
        // Arrange
        CreateUserRequest createUserRequest = CREATE_USER_REQUEST.toBuilder().surname(surname).build();

        // Act & Assert
        assertBadRequest(createUserRequest, "surname: Surname must not be blank");
    }

    @Test
    void create_shouldReturnBadRequestWhenSurnameIsTooLong() throws Exception {
        // Arrange
        CreateUserRequest createUserRequest = CREATE_USER_REQUEST.toBuilder().surname(TOO_LONG_VALUE).build();

        // Act & Assert
        assertBadRequest(createUserRequest, "surname: Surname can at most contain 255 symbols");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void create_shouldReturnBadRequestWhenEmailIsAbsent(String email) throws Exception {
        // Arrange
        CreateUserRequest createUserRequest = CREATE_USER_REQUEST.toBuilder().email(email).build();

        // Act & Assert
        assertBadRequest(createUserRequest, "email: Email must be specified");
    }

    @ParameterizedTest
    @MethodSource("com.microservices.margo.user_service.data.UserData#incorrectEmails")
    void create_shouldReturnBadRequestWhenEmailIsInvalid(String email) throws Exception {
        // Arrange
        CreateUserRequest createUserRequest = CREATE_USER_REQUEST.toBuilder().email(email).build();

        // Act & Assert
        assertBadRequest(createUserRequest, "email: must be a well-formed email address");
    }

    @ParameterizedTest
    @MethodSource("com.microservices.margo.user_service.data.UserData#incorrectPhones")
    void create_shouldReturnBadRequestWhenPhoneIsInvalid(String phone) throws Exception {
        // Arrange
        CreateUserRequest createUserRequest = CREATE_USER_REQUEST.toBuilder().phone(phone).build();

        // Act & Assert
        assertBadRequest(createUserRequest, "phone: Phone number must be 7–20 digits and may include +, spaces, dashes, or parentheses");
    }

    @Test
    void create_shouldPropagateUserExistExceptionsFromServiceAs409() throws Exception {
        // Arrange
        String errorMessage = "User exists!";
        when(createUserUseCase.execute(CREATE_USER_REQUEST)).thenThrow(new UserAlreadyExistsException(errorMessage));

        // Act & Assert
        mockMvc.perform(post(USER_PATH).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CREATE_USER_REQUEST)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(errorMessage));
    }

    @Test
    void getById_shouldReturnUser() throws Exception {
        // Arrange
        when(getUserUseCase.execute(USER.id())).thenReturn(USER);

        // Act & Assert
        andExpectUser(mockMvc.perform(get(USER_PATH + "/" + USER.id()))
                .andExpect(status().isOk()));

    }

    @Test
    void getById_shouldPropagateNotFoundAs404() throws Exception {
        // Arrange
        String errorMessage = "Not found!";
        when(getUserUseCase.execute(USER.id())).thenThrow(new EntityNotFoundException(errorMessage));

        // Act & Assert
        mockMvc.perform(get(USER_PATH + "/" + USER.id()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(errorMessage));
    }

    private void assertBadRequest(CreateUserRequest createUserRequest, String expectedErrorMessage) throws Exception {
        mockMvc.perform(post(USER_PATH).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createUserRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(expectedErrorMessage));
        verifyNoInteractions(createUserUseCase);
    }

    private ResultActions andExpectUser(ResultActions resultActions) throws Exception {
        return resultActions
                .andExpect(jsonPath("$.id").value(USER.id().toString()))
                .andExpect(jsonPath("$.name").value(USER.name()))
                .andExpect(jsonPath("$.surname").value(USER.surname()))
                .andExpect(jsonPath("$.birthDate").value(USER.birthDate().toString()))
                .andExpect(jsonPath("$.createdAt").value(USER.createdAt().toString()))
                .andExpect(jsonPath("$.email").value(USER.email()))
                .andExpect(jsonPath("$.phone").value(USER.phone()));
    }
}