package com.microservices.margo.user_service.core.domain.validation;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class MinAgeValidatorTest {

    private final MinAgeValidator validator = new MinAgeValidator();

    @Test
    void isValid_returnsTrue_whenOldEnough() {
        // person born exactly MIN_AGE years ago is valid
        LocalDate birthDate = LocalDate.now().minusYears(ValidationConstants.MIN_AGE);
        assertThat(validator.isValid(birthDate, null)).isTrue();
    }

    @Test
    void isValid_returnsTrue_whenWellAboveMinAge() {
        LocalDate birthDate = LocalDate.now().minusYears(30);
        assertThat(validator.isValid(birthDate, null)).isTrue();
    }

    @Test
    void isValid_returnsFalse_whenTooYoung() {
        LocalDate birthDate = LocalDate.now().minusYears(ValidationConstants.MIN_AGE - 1);
        assertThat(validator.isValid(birthDate, null)).isFalse();
    }

    @Test
    void isValid_returnsFalse_whenNull() {
        assertThat(validator.isValid(null, null)).isFalse();
    }

    @Test
    void isValid_returnsFalse_whenFutureDate() {
        assertThat(validator.isValid(LocalDate.now().plusDays(1), null)).isFalse();
    }
}