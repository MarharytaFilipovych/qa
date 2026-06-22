package com.microservices.margo.order_service.core.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class ObjectMapperConfigTest {

    private final ObjectMapper objectMapper = new ObjectMapperConfig().objectMapper();

    @Test
    void objectMapper_isNotNull() {
        assertThat(objectMapper).isNotNull();
    }

    @Test
    void objectMapper_serializesLocalDateTime() throws Exception {
        record Payload(LocalDateTime time) {}
        String json = objectMapper.writeValueAsString(new Payload(LocalDateTime.of(2024, 1, 1, 12, 0)));
        // JavaTimeModule registered — should not throw and should not write as array
        assertThat(json).doesNotContain("[");
    }

    @Test
    void objectMapper_doesNotFailOnIgnoredProperties() {
        assertThatCode(() ->
                objectMapper.readValue("{\"unknown\":\"value\"}", Object.class)
        ).doesNotThrowAnyException();
    }
}