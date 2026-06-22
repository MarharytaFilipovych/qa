package com.microservices.margo.user_service.core.application.usecase;

import com.microservices.margo.user_service.core.application.mapper.UserMapper;
import com.microservices.margo.user_service.core.domain.User;
import com.microservices.margo.user_service.core.infrastructure.entity.UserEntity;
import com.microservices.margo.user_service.core.infrastructure.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetUserUseCaseTest {

    @Mock UserRepository userRepository;
    @Mock UserMapper userMapper;

    @InjectMocks GetUserUseCase useCase;

    @Test
    void execute_returnsUser_whenFound() {
        UUID id = UUID.randomUUID();
        UserEntity entity = UserEntity.builder().id(id).build();
        User user = new User(id, "Jane", "Doe", null, LocalDate.of(1995, 1, 1), "jane@example.com", null);

        when(userRepository.findById(id)).thenReturn(Optional.of(entity));
        when(userMapper.toDomain(entity)).thenReturn(user);

        assertThat(useCase.execute(id)).isEqualTo(user);
    }

    @Test
    void execute_throwsEntityNotFound_whenMissing() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(id))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(id.toString());
    }
}