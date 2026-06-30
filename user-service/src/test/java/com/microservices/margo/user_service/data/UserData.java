package com.microservices.margo.user_service.data;

import com.microservices.margo.user_service.core.application.request.CreateUserRequest;
import com.microservices.margo.user_service.core.domain.User;
import com.microservices.margo.user_service.core.infrastructure.entity.UserEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.stream.Stream;

import static com.microservices.margo.user_service.core.domain.validation.ValidationConstants.MIN_AGE;

public final class UserData {
    private static final String NAME = "Marharyta";
    private static final String SURNAME = "Filipovych";
    private static final String PHONE = "+380971519425";
    private static final String EMAIL = "margosha@gmail.com";
    private static final LocalDate BIRTH_DATE = LocalDate.of(2006, 8, 2);
    private static final LocalDateTime CREATED_AT = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    public static final UUID USER_ID = UUID.randomUUID();
    public static final String TOO_LONG_VALUE = "b".repeat(256);

    public static User getUser() {
        return User.builder()
                .id(USER_ID)
                .name(NAME)
                .surname(SURNAME)
                .phone(PHONE)
                .birthDate(BIRTH_DATE)
                .email(EMAIL)
                .createdAt(CREATED_AT)
                .build();
    }

    public static CreateUserRequest createUserRequest() {
        return CreateUserRequest.builder()
                .name(NAME)
                .surname(SURNAME)
                .phone(PHONE)
                .birthDate(BIRTH_DATE)
                .email(EMAIL)
                .build();
    }

    public static UserEntity getUserEntity() {
        return UserEntity.builder()
                .id(USER_ID)
                .name(NAME)
                .surname(SURNAME)
                .phone(PHONE)
                .birthDate(BIRTH_DATE)
                .email(EMAIL)
                .createdAt(CREATED_AT)
                .build();
    }

    static Stream<LocalDate> underageBirthDates() {
        return Stream.of(
                LocalDate.now().minusYears(13),
                LocalDate.now(),
                LocalDate.now().plusDays(1)
        );
    }

    static Stream<LocalDate> adultBirthDates() {
        return Stream.of(
                LocalDate.now().minusYears(MIN_AGE),
                LocalDate.now().minusYears(18),
                LocalDate.of(1990, 1, 1)
        );
    }

    static Stream<String> incorrectPhones() {
        return Stream.of("123", TOO_LONG_VALUE, "abc-def-ghij",
                "123@456", "");
    }

    static Stream<String> incorrectEmails() {
        return Stream.of("a".repeat(91) + "@gmail.com", "hiiiiiiiii");
    }
}
