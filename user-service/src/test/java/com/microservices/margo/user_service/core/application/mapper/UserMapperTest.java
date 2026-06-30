package com.microservices.margo.user_service.core.application.mapper;

import com.microservices.margo.user_service.core.application.request.CreateUserRequest;
import com.microservices.margo.user_service.core.domain.User;
import com.microservices.margo.user_service.core.infrastructure.entity.UserEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static com.microservices.margo.user_service.data.UserData.createUserRequest;
import static com.microservices.margo.user_service.data.UserData.getUser;
import static com.microservices.margo.user_service.data.UserData.getUserEntity;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DisplayName("UserMapper tests")
class UserMapperTest {

    private final UserMapper mapper = Mappers.getMapper(UserMapper.class);

    @Test
    void toEntity_shouldMapToDomain() {
        // Arrange
        CreateUserRequest request = createUserRequest();
        UserEntity expectedUserEntity = getUserEntity().toBuilder()
                .id(null)
                .createdAt(null)
                .build();

        // Act
        UserEntity userEntity = mapper.toEntity(request);

        // Assert
        assertThat(userEntity).isEqualTo(expectedUserEntity);
    }

    @Test
    void toEntity_ifRequestIsNull_shouldReturnNull() {
        // Act & Assert
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    void toDomain_shouldMapToEntity() {
        // Arrange
        User expectedUser = getUser();

        // Act
        User user = mapper.toDomain(getUserEntity());

        // Assert
        assertThat(user).isEqualTo(expectedUser);
    }

    @Test
    void toDomain_ifEntityIsNull_shouldReturnNull() {
        // Act & Assert
        assertThat(mapper.toDomain(null)).isNull();
    }
}