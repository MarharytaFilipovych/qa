package com.microservices.margo.user_service.core.application.usecase;

import com.microservices.margo.user_service.core.application.mapper.UserMapper;
import com.microservices.margo.user_service.core.domain.User;
import com.microservices.margo.user_service.core.infrastructure.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetUserUseCase {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public User execute(UUID id) {
        log.info("Fetching user {}", id);
        return userRepository.findById(id)
                .map(userMapper::toDomain)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
    }
}
