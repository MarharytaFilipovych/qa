package com.microservices.margo.order_service.api;

import com.microservices.margo.order_service.core.application.request.CreateOrderRequest;
import com.microservices.margo.order_service.core.application.request.UpdateOrderStatusRequest;
import com.microservices.margo.order_service.core.application.usecase.CreateOrderUseCase;
import com.microservices.margo.order_service.core.application.usecase.GetOrderUseCase;
import com.microservices.margo.order_service.core.application.usecase.UpdateOrderStatusUseCase;
import com.microservices.margo.order_service.core.domain.Order;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import java.net.URI;
import java.util.UUID;

@Validated
@RestController
@RequestMapping({"/orders", "/orders/"})
@RequiredArgsConstructor
public class OrderController {
    private final CreateOrderUseCase createOrder;
    private final GetOrderUseCase getOrder;
    private final UpdateOrderStatusUseCase updateStatus;

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreateOrderRequest request){
        Order order = createOrder.execute(request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}").build(order.id());
        return ResponseEntity.created(location).body(order);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable UUID id){
        return ResponseEntity.ok(getOrder.execute(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable UUID id, @Valid @RequestBody UpdateOrderStatusRequest request){
        updateStatus.execute(id, request);
        return ResponseEntity.noContent().build();
    }
}
