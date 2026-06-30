package com.microservices.margo.order_service.core.application.mapper;

import com.microservices.margo.order_service.core.application.request.CreateOrderRequest;
import com.microservices.margo.order_service.core.domain.Order;
import com.microservices.margo.order_service.core.infrastructure.entity.OrderEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static com.microservices.margo.order_service.data.OrderData.createOrderRequest;
import static com.microservices.margo.order_service.data.OrderData.getOrder;
import static com.microservices.margo.order_service.data.OrderData.getOrderEntity;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OrderMapper tests")
class OrderMapperTest {

    private static final Order ORDER = getOrder();
    private static final OrderEntity ORDER_ENTITY = getOrderEntity();

    private final OrderMapper mapper = Mappers.getMapper(OrderMapper.class);

    @Test
    void toEntity_fromCreateOrderRequest_shouldSetStatusToPending() {
        // Arrange
        CreateOrderRequest request = createOrderRequest();
        OrderEntity expectedEntity = ORDER_ENTITY.toBuilder()
                .id(null)
                .createdAt(null)
                .build();

        // Act
        OrderEntity entity = mapper.toEntity(request);

        // Assert
        assertThat(entity).isEqualTo(expectedEntity);
    }

    @Test
    void toEntity_fromCreateOrderRequest_ifRequestIsNull_shouldReturnNull() {
        // Act & Assert
        assertThat(mapper.toEntity((CreateOrderRequest) null)).isNull();
    }

    @Test
    void toDomain_shouldMapEntityToDomain() {
        // Act
        Order result = mapper.toDomain(ORDER_ENTITY);

        // Assert
        assertThat(result).isEqualTo(ORDER);
    }

    @Test
    void toDomain_ifEntityIsNull_shouldReturnNull() {
        // Act & Assert
        assertThat(mapper.toDomain(null)).isNull();
    }

    @Test
    void toEntity_fromOrder_shouldMapAllFields() {
        // Act
        OrderEntity entity = mapper.toEntity(ORDER);

        // Assert
        assertThat(entity).isEqualTo(ORDER_ENTITY);
    }

    @Test
    void toEntity_fromOrder_ifOrderIsNull_shouldReturnNull() {
        // Act & Assert
        assertThat(mapper.toEntity((Order) null)).isNull();
    }
}