package com.microservices.margo.user_service.contract;

import io.restassured.response.Response;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.UUID;

import static com.microservices.margo.user_service.data.UserData.TOO_LONG_VALUE;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

public class UserIT extends BaseIT {

    private static final String CREATE_USER_JSON = "integration/request/create_user.json";
    private static final String NAME = "name";
    private static final String PHONE = "phone";
    private static final String SURNAME = "surname";
    private static final String EMAIL = "email";
    private static final String BIRTH_DATE = "birthDate";

    @Test
    @SneakyThrows
    void createUser_shouldReturn201WithUserBody() {
        // Arrange
        String requestBody = getFileContent(CREATE_USER_JSON);

        // Act & Assert
        Response response = createUserSuccess(requestBody);

        assertThat(response.getHeader("Location")).contains(USERS_URL + SLASH + getIdFromResponse(response));
        JSONAssert.assertEquals(requestBody, response.getBody().asString(), JSONCompareMode.LENIENT);
    }

    @Test
    @SneakyThrows
    void createUser_thenGetById_shouldReturnSameData() {
        // Arrange
        String requestBody = getFileContent(CREATE_USER_JSON);
        Response createResponse = createUserSuccess(requestBody);
        UUID userId = getIdFromResponse(createResponse);

        // Act & Assert
        Response getResponse = given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .get(USERS_URL + SLASH + userId);

        assertThat(getResponse.getStatusCode()).isEqualTo(200);
        JSONAssert.assertEquals(requestBody, getResponse.getBody().asString(), JSONCompareMode.LENIENT);
    }

    @ParameterizedTest
    @SneakyThrows
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\n", })
    void createUser_whenNameIsBlank_shouldReturn400(String name) {
        // Arrange
        String requestBody = withField(getFileContent(CREATE_USER_JSON), NAME, name);

        // Act & Assert
        createUserFailure(requestBody);
    }

    @Test
    void createUser_whenNameIsTooLong_shouldReturn400() {
        // Arrange
        String requestBody = withField(getFileContent(CREATE_USER_JSON), NAME, TOO_LONG_VALUE);

        // Act & Assert
        createUserFailure(requestBody);
    }

    @ParameterizedTest
    @SneakyThrows
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\n", })
    void createUser_whenSurnameIsBlank_shouldReturn400(String surname) {
        // Arrange
        String requestBody = withField(getFileContent(CREATE_USER_JSON), SURNAME, surname);

        // Act & Assert
        createUserFailure(requestBody);
    }

    @Test
    void createUser_whenSurnameIsTooLong_shouldReturn400() {
        // Arrange
        String requestBody = withField(getFileContent(CREATE_USER_JSON), SURNAME, TOO_LONG_VALUE);

        // Act & Assert
        createUserFailure(requestBody);
    }

    @ParameterizedTest
    @SneakyThrows
    @NullAndEmptySource
    @MethodSource("com.microservices.margo.user_service.data.UserData#incorrectEmails")
    void createUser_whenEmailIsInvalid_shouldReturn400(String email) {
        // Arrange
        String requestBody = withField(getFileContent(CREATE_USER_JSON), EMAIL, email);

        // Act & Assert
        createUserFailure(requestBody);
    }

    @ParameterizedTest
    @MethodSource("com.microservices.margo.user_service.data.UserData#underageBirthDates")
    @SneakyThrows
    void createUser_whenUserIsTooYoung_shouldReturn400(LocalDate date) {
        // Arrange
        String requestBody = withField(getFileContent(CREATE_USER_JSON), BIRTH_DATE, date.toString());

        // Act & Assert
        createUserFailure(requestBody);
    }

    @ParameterizedTest
    @MethodSource("com.microservices.margo.user_service.data.UserData#incorrectPhones")
    void createUser_whenPhoneIsInvalid_shouldReturn400(String phone) {
        // Arrange
        String requestBody = withField(getFileContent(CREATE_USER_JSON), PHONE, phone);

        // Act & Assert
        createUserFailure(requestBody);
    }

    @Test
    @SneakyThrows
    void getUser_whenExists_shouldReturnThisUser() {
        // Arrange
        String expectedUser = getFileContent(CREATE_USER_JSON);
        UUID id = getIdFromResponse(createUserSuccess(expectedUser));

        // Act & Assert
        Response response = given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .get(USERS_URL + SLASH + id);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK.value());
        JSONAssert.assertEquals(expectedUser, response.getBody().asString(), JSONCompareMode.LENIENT);
    }

    @Test
    @SneakyThrows
    void getUser_whenNotFound_shouldReturn404() {
        // Arrange
        Response response = given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .get(USERS_URL + SLASH + UUID.randomUUID());

        // Act & Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
    }
}