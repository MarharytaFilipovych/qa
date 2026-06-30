package com.microservices.margo.workflow_service.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservices.margo.workflow_service.core.application.request.CreateOrderRequest;
import com.microservices.margo.workflow_service.core.application.usecase.GetWorkflowUseCase;
import com.microservices.margo.workflow_service.core.application.usecase.StartCreateOrderWorkflowUseCase;
import com.microservices.margo.workflow_service.core.domain.Workflow;
import jakarta.persistence.EntityNotFoundException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;

import static com.microservices.margo.workflow_service.data.WorkflowData.createOrderRequest;
import static com.microservices.margo.workflow_service.data.WorkflowData.getWorkflow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WorkflowController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("WorkflowController tests")
class WorkflowControllerTest {

    private static final String WORKFLOWS_PATH = "/workflows";
    private static final String CREATE_ORDER_SUB_PATH = "/create-order";

    private static final Workflow WORKFLOW = getWorkflow();
    private static final CreateOrderRequest CREATE_ORDER_REQUEST = createOrderRequest();

    @MockitoBean
    private StartCreateOrderWorkflowUseCase startCreateOrderWorkflow;

    @MockitoBean
    private GetWorkflowUseCase getWorkflow;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @SneakyThrows
    void startPlaceOrder_shouldReturn202AndWorkflow() {
        // Arrange
        when(startCreateOrderWorkflow.execute(CREATE_ORDER_REQUEST)).thenReturn(WORKFLOW);

        // Act & Assert
        andExpectWorkflow(
                mockMvc.perform(post(WORKFLOWS_PATH + CREATE_ORDER_SUB_PATH)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(CREATE_ORDER_REQUEST)))
                        .andExpect(status().isAccepted())
        );
    }

    @Test
    @SneakyThrows
    void startPlaceOrder_shouldReturn400WhenOwnerUserIdIsNull() {
        // Arrange
        CreateOrderRequest request = CREATE_ORDER_REQUEST.toBuilder().ownerUserId(null).build();

        // Act & Assert
        assertBadRequest(request, "ownerUserId: ownerUserId is required");
    }

    @ParameterizedTest
    @SneakyThrows
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\n", "\t"})
    void startPlaceOrder_shouldReturn400WhenItemNameIsBlank(String itemName) {
        // Arrange
        CreateOrderRequest request = CREATE_ORDER_REQUEST.toBuilder().itemName(itemName).build();

        // Act & Assert
        assertBadRequest(request, "itemName: itemName is required");
    }

    @Test
    @SneakyThrows
    void startPlaceOrder_shouldReturn400WhenQuantityIsZero() {
        // Arrange
        CreateOrderRequest request = CREATE_ORDER_REQUEST.toBuilder().quantity(0).build();

        // Act & Assert
        assertBadRequest(request, "quantity: quantity must be at least 1");
    }

    @Test
    @SneakyThrows
    void startPlaceOrder_shouldReturn400WhenPriceIsNull() {
        // Arrange
        CreateOrderRequest request = CREATE_ORDER_REQUEST.toBuilder().price(null).build();

        // Act & Assert
        assertBadRequest(request, "price: price is required");
    }

    @Test
    @SneakyThrows
    void startPlaceOrder_shouldReturn400WhenPriceIsNegative() {
        // Arrange
        CreateOrderRequest request = CREATE_ORDER_REQUEST.toBuilder().price(new BigDecimal("-1")).build();

        // Act & Assert
        assertBadRequest(request, "price: price cannot be negative");
    }

    @Test
    @SneakyThrows
    void getById_shouldReturnWorkflow() {
        // Arrange
        when(getWorkflow.execute(WORKFLOW.id())).thenReturn(WORKFLOW);

        // Act & Assert
        andExpectWorkflow(
                mockMvc.perform(get(WORKFLOWS_PATH + "/" + WORKFLOW.id()))
                        .andExpect(status().isOk())
        );
    }

    @Test
    @SneakyThrows
    void getById_shouldReturn404WhenWorkflowNotFound() {
        // Arrange
        String errorMessage = "Workflow not found: " + WORKFLOW.id();
        when(getWorkflow.execute(WORKFLOW.id())).thenThrow(new EntityNotFoundException(errorMessage));

        // Act & Assert
        mockMvc.perform(get(WORKFLOWS_PATH + "/" + WORKFLOW.id()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(errorMessage));
    }

    @Test
    @SneakyThrows
    @DisplayName("getById should return 400 when workflowId is not a valid UUID")
    void getById_shouldReturn400WhenIdIsInvalid() {
        // Act & Assert
        mockMvc.perform(get(WORKFLOWS_PATH + "/not-a-uuid"))
                .andExpect(status().isBadRequest());
    }

    private void assertBadRequest(CreateOrderRequest request, String message) throws Exception {
        mockMvc.perform(post(WORKFLOWS_PATH + CREATE_ORDER_SUB_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(message));
    }

    private void andExpectWorkflow(ResultActions actions) throws Exception {
        actions
                .andExpect(jsonPath("$.id").value(WORKFLOW.id().toString()))
                .andExpect(jsonPath("$.type").value(WORKFLOW.type()))
                .andExpect(jsonPath("$.state").value(WORKFLOW.state().name()));
    }
}