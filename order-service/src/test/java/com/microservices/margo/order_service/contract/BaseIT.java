package com.microservices.margo.order_service.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.microservices.margo.order_service.core.infrastructure.repository.OrderRepository;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.util.Objects;
import java.util.Properties;
import java.util.UUID;

import static com.microservices.margo.order_service.data.Constants.SLASH;
import static io.restassured.RestAssured.given;
import static java.nio.charset.StandardCharsets.UTF_8;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Import(TestRabbitConfig.class)
public class BaseIT {

    public static final String ORDERS_URL = "/api/orders";

    protected static WireMock userServiceWiremock;

    @Autowired
    private OrderRepository orderRepository;

    @SneakyThrows
    @BeforeAll
    static void beforeAll() {
        Properties props = new Properties();
        props.load(ClassLoader.getSystemResourceAsStream("integration/contract-tests.properties"));

        int wiremockPort = Integer.parseInt(props.getProperty("user.service.wiremock.port"));

        WireMockServer userServiceServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(wiremockPort));
        userServiceServer.start();

        userServiceWiremock = new WireMock("localhost", wiremockPort);

        RestAssured.port = Integer.parseInt(props.getProperty("application.port"));
        RestAssured.baseURI = props.getProperty("application.baseUri");
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @BeforeEach
    void prepare() {
        userServiceWiremock.resetMappings();
        orderRepository.deleteAll();
    }

    protected Response createOrderSuccess(String requestBody) {
        return createOrder(requestBody, HttpStatus.CREATED.value());
    }

    protected void createOrderFailure(String requestBody) {
        createOrder(requestBody, HttpStatus.BAD_REQUEST.value());
    }

    protected Response createOrder(String requestBody, int expectedStatus) {
        return given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(requestBody)
                .post(ORDERS_URL)
                .then()
                .statusCode(expectedStatus)
                .extract().response();
    }

    protected void updateOrderStatusFailure(UUID orderId, String requestBody) {
        updateOrderStatus(orderId, requestBody, HttpStatus.BAD_REQUEST.value());
    }

    protected void updateOrderStatusSuccess(UUID orderId, String requestBody) {
        updateOrderStatus(orderId, requestBody, HttpStatus.NO_CONTENT.value());
    }

    protected void updateOrderStatus(UUID orderId, String requestBody, int expectedStatus) {
        given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(requestBody)
                .patch(ORDERS_URL + SLASH + orderId + "/status")
                .then()
                .statusCode(expectedStatus);
    }

    @SneakyThrows
    protected String getFileContent(String path) {
        return IOUtils.toString(Objects.requireNonNull(getClass().getClassLoader().getResource(path)), UTF_8);
    }

    @SneakyThrows
    protected UUID getIdFromResponse(Response response) {
        String raw = new ObjectMapper().reader()
                .readTree(response.getBody().asString())
                .get("id").asText();
        return UUID.fromString(raw);
    }

    @SneakyThrows
    protected  <T> String withField(String json, String field, T value) {
        ObjectMapper mapper = new ObjectMapper();
        var node = (ObjectNode) mapper.readTree(json);
        if (value == null) {
            node.putNull(field);
        } else {
            node.putPOJO(field, value);
        }
        return mapper.writeValueAsString(node);
    }
}
