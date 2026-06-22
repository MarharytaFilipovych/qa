package com.microservices.margo.workflow_service.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservices.margo.workflow_service.core.application.usecase.GetWorkflowUseCase;
import com.microservices.margo.workflow_service.core.application.usecase.StartCreateOrderWorkflowUseCase;
import com.microservices.margo.workflow_service.core.domain.Workflow;
import com.microservices.margo.workflow_service.core.domain.WorkflowState;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WorkflowController.class)
class WorkflowControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean StartCreateOrderWorkflowUseCase startPlaceOrderWorkflow;
    @MockBean GetWorkflowUseCase getWorkflow;

    private Workflow sampleWorkflow(UUID id) {
        return Workflow.builder().id(id).state(WorkflowState.COMPLETED).build();
    }

    private String validCreateBody() {
        return """
                {"ownerUserId":"%s","itemName":"Latte","quantity":2,"price":5.99}
                """.formatted(UUID.randomUUID());
    }

    // --- POST /workflows/create-order ---

    @Test
    void startPlaceOrder_returns202() throws Exception {
        UUID id = UUID.randomUUID();
        when(startPlaceOrderWorkflow.execute(any())).thenReturn(sampleWorkflow(id));

        mockMvc.perform(post("/workflows/create-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateBody()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void startPlaceOrder_returns400_whenOwnerUserIdNull() throws Exception {
        String body = """
                {"ownerUserId":null,"itemName":"Latte","quantity":2,"price":5.99}
                """;
        mockMvc.perform(post("/workflows/create-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void startPlaceOrder_returns400_whenItemNameBlank() throws Exception {
        String body = """
                {"ownerUserId":"%s","itemName":"","quantity":2,"price":5.99}
                """.formatted(UUID.randomUUID());
        mockMvc.perform(post("/workflows/create-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void startPlaceOrder_returns400_whenQuantityZero() throws Exception {
        String body = """
                {"ownerUserId":"%s","itemName":"Latte","quantity":0,"price":5.99}
                """.formatted(UUID.randomUUID());
        mockMvc.perform(post("/workflows/create-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void startPlaceOrder_returns400_whenPriceNegative() throws Exception {
        String body = """
                {"ownerUserId":"%s","itemName":"Latte","quantity":2,"price":-1}
                """.formatted(UUID.randomUUID());
        mockMvc.perform(post("/workflows/create-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void startPlaceOrder_returns400_whenInvalidJson() throws Exception {
        mockMvc.perform(post("/workflows/create-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{bad json"))
                .andExpect(status().isBadRequest());
    }

    // --- GET /workflows/{workflowId} ---

    @Test
    void getById_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(getWorkflow.execute(id)).thenReturn(sampleWorkflow(id));

        mockMvc.perform(get("/workflows/{workflowId}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void getById_returns404_whenNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(getWorkflow.execute(id)).thenThrow(new EntityNotFoundException("Workflow not found: " + id));

        mockMvc.perform(get("/workflows/{workflowId}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void getById_returns400_whenIdNotUuid() throws Exception {
        mockMvc.perform(get("/workflows/not-a-uuid"))
                .andExpect(status().isBadRequest());
    }
}