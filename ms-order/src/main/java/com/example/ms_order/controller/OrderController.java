package com.example.ms_order.controller;

import com.example.ms_order.dto.CreateOrderRequest;
import com.example.ms_order.dto.OrderDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Order", description = "주문 관리 API")
public class OrderController {

    private final Map<Long, OrderDto> store = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(1);

    @GetMapping
    @Operation(summary = "전체 주문 목록 조회", operationId = "listOrders")
    public List<OrderDto> listOrders() {
        return List.copyOf(store.values());
    }

    @GetMapping("/{id}")
    @Operation(summary = "주문 상세 조회", operationId = "getOrder")
    public ResponseEntity<OrderDto> getOrder(
            @Parameter(description = "주문 ID") @PathVariable Long id) {
        OrderDto order = store.get(id);
        return order != null ? ResponseEntity.ok(order) : ResponseEntity.notFound().build();
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "사용자별 주문 조회", operationId = "getOrdersByUser")
    public List<OrderDto> getOrdersByUser(
            @Parameter(description = "사용자 ID") @PathVariable Long userId) {
        return store.values().stream()
                .filter(o -> o.userId().equals(userId))
                .toList();
    }

    @PostMapping
    @Operation(summary = "주문 생성", operationId = "createOrder")
    public OrderDto createOrder(@RequestBody CreateOrderRequest request) {
        long id = sequence.getAndIncrement();
        int total = request.items().stream()
                .mapToInt(i -> i.quantity() * i.unitPrice())
                .sum();
        OrderDto order = new OrderDto(id, request.userId(), request.items(), total, "CONFIRMED", Instant.now());
        store.put(id, order);
        return order;
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "주문 취소", operationId = "cancelOrder")
    public ResponseEntity<OrderDto> cancelOrder(
            @Parameter(description = "주문 ID") @PathVariable Long id) {
        OrderDto existing = store.get(id);
        if (existing == null) return ResponseEntity.notFound().build();
        OrderDto cancelled = new OrderDto(existing.id(), existing.userId(), existing.items(),
                existing.totalAmount(), "CANCELLED", existing.orderedAt());
        store.put(id, cancelled);
        return ResponseEntity.ok(cancelled);
    }
}
