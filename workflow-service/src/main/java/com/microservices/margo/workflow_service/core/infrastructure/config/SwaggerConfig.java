package com.microservices.margo.workflow_service.core.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI(){
        return new OpenAPI().info(new Info()
                        .title("Workflow Service API")
                        .description("Saga / Process Manager for place-order workflow")
                        .version("1.0.0"));
    }
}
