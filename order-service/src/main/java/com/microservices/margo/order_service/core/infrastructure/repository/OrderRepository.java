package com.microservices.margo.order_service.core.infrastructure.repository;

import com.microservices.margo.order_service.core.infrastructure.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@SuppressWarnings("NullableProblems")
@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {
}
