package com.microservices.margo.user_service.api;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@RestController
@RequestMapping({"/health", "/health/"})
@RequiredArgsConstructor
public class HealthController {
    private final HealthEndpoint healthEndpoint;

    @GetMapping
    public ResponseEntity<?> health() {
        HealthComponent healthComponent = healthEndpoint.health();

        HttpStatus status = (healthComponent.getStatus() == Status.UP)
                ? OK : SERVICE_UNAVAILABLE;

        return ResponseEntity.status(status).body(healthComponent);
    }
}