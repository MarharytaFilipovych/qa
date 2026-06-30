package com.microservices.margo.workflow_service.core.infrastructure.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("ObjectMapperConfig tests")
class ObjectMapperConfigTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapperConfig().objectMapper();
    }

    @Test
    @SneakyThrows
    void objectMapper_shouldMakeUseOfJavaTimeModule() {
        // Arrange
        LocalDateTime time = LocalDateTime.of(2006, 8, 2, 0, 34, 6);

        // Act
        LocalDateTime deserialized = objectMapper.readValue(
                objectMapper.writeValueAsString(time), LocalDateTime.class);

        // Assert
        assertThat(deserialized).isEqualTo(time);
    }

    @Test
    void objectMapper_shouldNotFailOnUnknownProperties() {
        // Arrange
        String json = "{\"name\":\"John\",\"unknownField\":\"value\"}";

        // Act & Assert
        assertThatNoException().isThrownBy(() ->
                objectMapper.readValue(json, Beauty.class));
    }

    @Test
    void objectMapper_shouldNotFailOnIgnoredProperties() {
        // Arrange
        String json = "{\"name\":\"John\",\"password\":\"secret\"}";

        // Act & Assert
        assertThatNoException().isThrownBy(() ->
                objectMapper.readValue(json, Beauty.class));
    }

    @Test
    void objectMapper_shouldSerializeDatesAsISOString() throws Exception {
        // Arrange
        LocalDateTime dateTime = LocalDateTime.of(2006, 8, 2, 12, 32, 15);

        // Act
        String json = objectMapper.writeValueAsString(dateTime);

        // Assert
        assertThat(json).isEqualTo("\"2006-08-02T12:32:15\"");
    }

    private record Beauty(String name, @JsonIgnore String password) { }
}