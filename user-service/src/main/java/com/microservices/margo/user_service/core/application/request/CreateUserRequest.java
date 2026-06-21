package com.microservices.margo.user_service.core.application.request;

import com.microservices.margo.user_service.core.domain.validation.MinAge;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

import static com.microservices.margo.user_service.core.domain.validation.ValidationConstants.MAX_EMAIL_LENGTH;
import static com.microservices.margo.user_service.core.domain.validation.ValidationConstants.MAX_NAME_LENGTH;

public record CreateUserRequest(

        @NotBlank(message = "Name must not be blank")
        @Size(max = MAX_NAME_LENGTH, message = "Name can at most contain 255 symbols")
        String name,

        @NotBlank(message = "Surname must not be blank")
        @Size(max = MAX_NAME_LENGTH, message = "Surname can at most contain 255 symbols")
        String surname,

        @Pattern(regexp = "^\\+?[\\d\\s\\-()]{7,20}$",
                message = "Phone number must be 7–20 digits and may include +, spaces, dashes, or parentheses")
        String phone,

        @MinAge
        LocalDate birthDate,

        @NotBlank (message = "Email must be specified")
        @Email
        @Size(max = MAX_EMAIL_LENGTH, message = "Email can at most contain 100 symbols")
        String email
) {}