package com.microservices.margo.user_service.core.application.usecase;

import com.microservices.margo.user_service.core.application.mapper.UserMapper;
import com.microservices.margo.user_service.core.domain.User;
import com.microservices.margo.user_service.core.infrastructure.entity.UserEntity;
import com.microservices.margo.user_service.core.infrastructure.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static com.microservices.margo.user_service.data.UserData.getUser;
import static com.microservices.margo.user_service.data.UserData.getUserEntity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetUserUseCase test")
class GetUserUseCaseTest {
    private static final UserEntity USER_ENTITY = getUserEntity();
    private static final User USER = getUser();

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    GetUserUseCase getUserUseCase;

    @Test
    void execute_shouldFindUserByIdAndReturnIt() {
        // Arrange
        when(userRepository.findById(USER.id())).thenReturn(Optional.of(USER_ENTITY));
        when(userMapper.toDomain(USER_ENTITY)).thenReturn(USER);

        // Act
        User user = getUserUseCase.execute(USER.id());

        // Assert
        assertThat(user).isEqualTo(USER);
        verify(userMapper).toDomain(USER_ENTITY);
        verify(userRepository).findById(USER.id());
    }

    @Test
    void execute_shouldThrowWhenUserIsNotFound() {
        // Arrange
        when(userRepository.findById(USER.id())).thenReturn(Optional.empty());
        UUID userId = USER.id();

        // Act & Assert
        assertThatThrownBy(() -> getUserUseCase.execute(userId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("User not found: " + userId);
        verify(userMapper, never()).toDomain(USER_ENTITY);
        verify(userRepository).findById(userId);
    }
}