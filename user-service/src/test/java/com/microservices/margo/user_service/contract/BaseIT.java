package com.microservices.margo.user_service.contract;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.microservices.margo.user_service.core.infrastructure.repository.UserRepository;
import io.micrometer.core.instrument.util.IOUtils;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import static io.restassured.RestAssured.given;
import static java.nio.charset.StandardCharsets.UTF_8;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class BaseIT {

    public static final String USERS_URL = "/api/users";
    public static final String SLASH = "/";

    @Autowired
    private UserRepository userRepository;

    @SneakyThrows
    @BeforeAll
    static void beforeAll() {
        Properties props = new Properties();
        props.load(ClassLoader.getSystemResourceAsStream("integration/contract-tests.properties"));

        RestAssured.port = Integer.parseInt(props.getProperty("application.port"));
        RestAssured.baseURI = props.getProperty("application.baseUri");
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @BeforeEach
    void prepare() {
        userRepository.deleteAll();
    }

    protected Response createUserSuccess(String requestBody) {
        return createUser(requestBody, HttpStatus.CREATED.value());
    }

    protected void createUserFailure(String requestBody) {
        createUser(requestBody, HttpStatus.BAD_REQUEST.value());
    }

    protected Response createUser(String requestBody, int expectedStatus) {
        return given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(requestBody)
                .post(USERS_URL)
                .then()
                .statusCode(expectedStatus)
                .extract().response();
    }

    @SneakyThrows
    protected String getFileContent(String path) {
        return IOUtils.toString(
                Objects.requireNonNull(getClass().getClassLoader().getResource(path)).openStream(), UTF_8);
    }

    @SneakyThrows
    protected UUID getIdFromResponse(Response response) {
        String raw = new ObjectMapper().reader()
                .readTree(response.getBody().asString())
                .get("id").asText();
        return UUID.fromString(raw);
    }

    @SneakyThrows
    protected String withField(String json, String field, String value) {
        ObjectMapper mapper = new ObjectMapper();
        var node = (ObjectNode) mapper.readTree(json);
        if (value == null) {
            node.putNull(field);
        } else {
            node.put(field, value);
        }
        return mapper.writeValueAsString(node);
    }
}