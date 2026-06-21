package com.microservices.margo.workflow_service.core.infrastructure.config;

import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
public class RestClientConfig {

    private final CorrelationProperties correlationProperties;

    @Value("${rest-client.connect-timeout-ms:3000}")
    private int connectTimeout;

    @Value("${rest-client.read-timeout-ms:5000}")
    private int readTimeout;

    @Bean
    public RestClient restClient() {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setConnectionRequestTimeout(readTimeout);

        return RestClient.builder()
                .requestFactory(factory)
                .requestInterceptor((request, body, execution) -> {
                    String correlationId = MDC.get(correlationProperties.key());
                    if (correlationId != null) {
                        request.getHeaders().set(correlationProperties.header(), correlationId);
                    }
                    return execution.execute(request, body);
                })
                .build();
    }
}