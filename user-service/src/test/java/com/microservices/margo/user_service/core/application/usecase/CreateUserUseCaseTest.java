package com.microservices.margo.user_service.core.application.usecase;

import com.microservices.margo.user_service.core.application.exception.UserAlreadyExistsException;
import com.microservices.margo.user_service.core.application.mapper.UserMapper;
import com.microservices.margo.user_service.core.application.request.CreateUserRequest;
import com.microservices.margo.user_service.core.domain.User;
import com.microservices.margo.user_service.core.infrastructure.entity.UserEntity;
import com.microservices.margo.user_service.core.infrastructure.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.microservices.margo.user_service.data.UserData.createUserRequest;
import static com.microservices.margo.user_service.data.UserData.getUser;
import static com.microservices.margo.user_service.data.UserData.getUserEntity;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CreateUserUseCase tests")
@ExtendWith(MockitoExtension.class)
class CreateUserUseCaseTest {
    private static final CreateUserRequest CREATE_USER_REQUEST = createUserRequest();
    private static final UserEntity USER_ENTITY = getUserEntity();
    private static final User USER = getUser();

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private CreateUserUseCase createUserUseCase;

    @Test
    void execute_shouldSaveAndReturnUser() {
        // Arrange
        when(userMapper.toEntity(CREATE_USER_REQUEST)).thenReturn(USER_ENTITY);
        when(userRepository.save(USER_ENTITY)).thenReturn(USER_ENTITY);
        when(userMapper.toDomain(USER_ENTITY)).thenReturn(USER);
        when(userRepository.existsByEmail(CREATE_USER_REQUEST.email())).thenReturn(false);

        // Act
        User user = createUserUseCase.execute(CREATE_USER_REQUEST);

        // Assert
        assertThat(user).isEqualTo(USER);
        verify(userRepository).save(USER_ENTITY);
        verify(userRepository).existsByEmail(CREATE_USER_REQUEST.email());
        verify(userMapper).toDomain(USER_ENTITY);
        verify(userMapper).toEntity(CREATE_USER_REQUEST);
    }

    @Test
    void execute_shouldThrowExceptionIfSuchUserExists() {
        // Arrange
        when(userRepository.existsByEmail(CREATE_USER_REQUEST.email())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> createUserUseCase.execute(CREATE_USER_REQUEST))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessage("User with email %s already exists!".formatted(CREATE_USER_REQUEST.email()));
        verify(userRepository).existsByEmail(CREATE_USER_REQUEST.email());
        verify(userRepository, never()).save(any(UserEntity.class));
        verifyNoInteractions(userMapper);
    }
}