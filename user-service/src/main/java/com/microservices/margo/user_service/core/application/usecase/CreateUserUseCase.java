package com.microservices.margo.user_service.core.application.usecase;

import com.microservices.margo.user_service.core.application.exception.UserAlreadyExistsException;
import com.microservices.margo.user_service.core.application.mapper.UserMapper;
import com.microservices.margo.user_service.core.application.request.CreateUserRequest;
import com.microservices.margo.user_service.core.domain.User;
import com.microservices.margo.user_service.core.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateUserUseCase {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public User execute(CreateUserRequest request) {
        log.info("Creating user with email {}", request.email());
        if (userRepository.existsByEmail(request.email())){
         throw new UserAlreadyExistsException("User with email " + request.email() + " already exists!");
        }
        var saved = userRepository.save(userMapper.toEntity(request));
        return userMapper.toDomain(saved);
    }
}