package com.microservices.margo.user_service.core.application.usecase;

import com.microservices.margo.user_service.core.application.exception.UserAlreadyExistsException;
import com.microservices.margo.user_service.core.application.mapper.UserMapper;
import com.microservices.margo.user_service.core.application.request.CreateUserRequest;
import com.microservices.margo.user_service.core.domain.User;
import com.microservices.margo.user_service.core.infrastructure.entity.UserEntity;
import com.microservices.margo.user_service.core.infrastructure.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateUserUseCaseTest {

    @Mock UserRepository userRepository;
    @Mock UserMapper userMapper;

    @InjectMocks CreateUserUseCase useCase;

    private CreateUserRequest request;
    private UserEntity savedEntity;
    private User mappedUser;

    @BeforeEach
    void setUp() {
        request = new CreateUserRequest("Jane", "Doe", null,
                LocalDate.of(1995, 1, 1), "jane@example.com");

        savedEntity = UserEntity.builder()
                .id(UUID.randomUUID())
                .name("Jane")
                .surname("Doe")
                .email("jane@example.com")
                .birthDate(LocalDate.of(1995, 1, 1))
                .build();

        mappedUser = new User(savedEntity.getId(), "Jane", "Doe", null,
                LocalDate.of(1995, 1, 1), "jane@example.com", null);
    }

    @Test
    void execute_savesAndReturnsUser_whenEmailIsNew() {
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(userMapper.toEntity(request)).thenReturn(savedEntity);
        when(userRepository.save(savedEntity)).thenReturn(savedEntity);
        when(userMapper.toDomain(savedEntity)).thenReturn(mappedUser);

        User result = useCase.execute(request);

        assertThat(result).isEqualTo(mappedUser);
        verify(userRepository).save(savedEntity);
    }

    @Test
    void execute_throwsUserAlreadyExists_whenEmailTaken() {
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(request))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("jane@example.com");

        verify(userRepository, never()).save(any());
    }
}