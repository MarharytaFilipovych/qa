package com.microservices.margo.user_service.core.domain.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;

import static com.microservices.margo.user_service.core.domain.validation.ValidationConstants.MIN_AGE;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MinAgeValidatorTest {

    private final MinAgeValidator validator = new MinAgeValidator();

    @BeforeEach
    void setUp() {
        MinAge annotation = mock(MinAge.class);
        when(annotation.value()).thenReturn(MIN_AGE);
        validator.initialize(annotation);
    }

    @ParameterizedTest
    @MethodSource("com.microservices.margo.user_service.data.UserData#adultBirthDates")
    void isValid_validAge_shouldReturnTrue(LocalDate birthDate) {
        // Act & Assert
        assertThat(validator.isValid(birthDate, null)).isTrue();
    }

    @Test
    void isValid_dateIsNull_shouldReturnFalse() {
        // Act & Assert
        assertThat(validator.isValid(null, null)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("com.microservices.margo.user_service.data.UserData#underageBirthDates")
    void isValid_underAge_shouldReturnFalse(LocalDate birthDate) {
        // Act & Assert
        assertThat(validator.isValid(birthDate, null)).isFalse();
    }
}