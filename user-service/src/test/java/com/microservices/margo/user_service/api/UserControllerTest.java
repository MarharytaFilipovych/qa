package com.microservices.margo.user_service.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservices.margo.user_service.core.application.exception.UserAlreadyExistsException;
import com.microservices.margo.user_service.core.application.usecase.CreateUserUseCase;
import com.microservices.margo.user_service.core.application.usecase.GetUserUseCase;
import com.microservices.margo.user_service.core.domain.User;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean CreateUserUseCase createUserUseCase;
    @MockBean GetUserUseCase getUserUseCase;

    private User sampleUser(UUID id) {
        return new User(id, "Jane", "Doe", null, LocalDate.of(1995, 1, 1), "jane@example.com", null);
    }

    private String validBody() {
        return """
                {"name":"Jane","surname":"Doe","birthDate":"1995-01-01","email":"jane@example.com"}
                """;
    }

    // --- POST /users ---

    @Test
    void create_returns201WithLocation() throws Exception {
        UUID id = UUID.randomUUID();
        when(createUserUseCase.execute(any())).thenReturn(sampleUser(id));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString(id.toString())));
    }

    @Test
    void create_returns409_whenEmailTaken() throws Exception {
        when(createUserUseCase.execute(any()))
                .thenThrow(new UserAlreadyExistsException("User with email jane@example.com already exists!"));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isConflict());
    }

    @Test
    void create_returns400_whenNameBlank() throws Exception {
        String body = """
                {"name":"","surname":"Doe","birthDate":"1995-01-01","email":"jane@example.com"}
                """;
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_returns400_whenEmailInvalid() throws Exception {
        String body = """
                {"name":"Jane","surname":"Doe","birthDate":"1995-01-01","email":"not-an-email"}
                """;
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_returns400_whenBirthDateMissing() throws Exception {
        String body = """
                {"name":"Jane","surname":"Doe","email":"jane@example.com"}
                """;
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_returns400_whenInvalidJson() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid"))
                .andExpect(status().isBadRequest());
    }

    // --- GET /users/{id} ---

    @Test
    void getById_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(getUserUseCase.execute(id)).thenReturn(sampleUser(id));

        mockMvc.perform(get("/users/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void getById_returns404_whenNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(getUserUseCase.execute(id)).thenThrow(new EntityNotFoundException("User not found: " + id));

        mockMvc.perform(get("/users/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void getById_returns400_whenIdNotUuid() throws Exception {
        mockMvc.perform(get("/users/not-a-uuid"))
                .andExpect(status().isBadRequest());
    }
}