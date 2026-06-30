package com.microservices.margo.user_service.core.infrastructure.repository;

import com.microservices.margo.user_service.core.infrastructure.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    boolean existsByEmail(String email);
}

