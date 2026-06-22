package com.microservices.margo.order_service.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HealthController.class)
class HealthControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean HealthEndpoint healthEndpoint;

    @Test
    void health_returns200_whenUp() throws Exception {
        when(healthEndpoint.health()).thenReturn(Health.up().build());

        mockMvc.perform(get("/health"))
                .andExpect(status().isOk());
    }

    @Test
    void health_returns503_whenDown() throws Exception {
        when(healthEndpoint.health()).thenReturn(Health.down().build());

        mockMvc.perform(get("/health"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void health_returns503_whenUnknownStatus() throws Exception {
        HealthComponent unknown = Health.unknown().build();
        when(healthEndpoint.health()).thenReturn(unknown);

        mockMvc.perform(get("/health"))
                .andExpect(status().isServiceUnavailable());
    }
}