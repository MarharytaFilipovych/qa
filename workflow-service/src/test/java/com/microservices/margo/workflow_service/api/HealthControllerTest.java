package com.microservices.margo.workflow_service.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.stream.Stream;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("HealthController tests")
@WebMvcTest(HealthController.class)
class HealthControllerTest {
    private static final String HEALTH_PATH = "/health";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HealthEndpoint healthEndpoint;

    @Test
    void whenHealthEndpointStatusIsUp_shouldReturnOk() throws Exception {
        // Arrange
        when(healthEndpoint.health()).thenReturn(Health.up().build());

        // Act & Assert
        mockMvc.perform(get(HEALTH_PATH))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @MethodSource("errorStatuses")
    void whenHealthEndpointStatusIsDown_shouldReturn503Code(Health health) throws Exception {
        // Arrange
        when(healthEndpoint.health()).thenReturn(health);

        // Act & Assert
        mockMvc.perform(get(HEALTH_PATH))
                .andExpect(status().isServiceUnavailable());
    }

    private static Stream<Health> errorStatuses() {
        return Stream.of(Health.unknown().build(),
                Health.down().build(),
                Health.status(Status.OUT_OF_SERVICE).build()
        );
    }
}