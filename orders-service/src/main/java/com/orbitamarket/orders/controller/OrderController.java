package com.orbitamarket.orders.controller;

import com.orbitamarket.orders.dto.OrderRequest;
import com.orbitamarket.orders.dto.OrderResponse;
import com.orbitamarket.orders.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<?> createOrder(
            @RequestBody OrderRequest request,
            HttpServletRequest httpRequest) {
        String userId = httpRequest.getHeader("X-User-Id");

        if (userId == null || userId.isBlank()) {
            return buildError(HttpStatus.BAD_REQUEST, "MISSING_USER_ID",
                    "X-User-Id header is required");
        }

        if (request.getPrice() == null || request.getPrice() <= 0) {
            return buildError(HttpStatus.BAD_REQUEST, "INVALID_PRICE",
                    "Price must be greater than zero");
        }

        if (request.getProductType() == null || request.getProductType().isBlank()) {
            return buildError(HttpStatus.BAD_REQUEST, "INVALID_PAYLOAD",
                    "product_type is required");
        }

        if (request.getPayload() == null || request.getPayload().isEmpty()) {
            return buildError(HttpStatus.BAD_REQUEST, "INVALID_PAYLOAD",
                    "payload is required");
        }

        try {
            OrderResponse response = orderService.createOrder(
                    userId,
                    request.getProductType(),
                    request.getPrice(),
                    request.getPayload()
            );
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return buildError(HttpStatus.BAD_REQUEST, "INVALID_PAYLOAD", e.getMessage());
        } catch (Exception e) {
            return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                    "Failed to create order: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getOrders(HttpServletRequest httpRequest) {
        String userId = httpRequest.getHeader("X-User-Id");

        if (userId == null || userId.isBlank()) {
            return buildError(HttpStatus.BAD_REQUEST, "MISSING_USER_ID",
                    "X-User-Id header is required");
        }

        try {
            List<OrderResponse> orders = orderService.getOrdersByUserId(userId);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                    "Failed to fetch orders: " + e.getMessage());
        }
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrder(
            @PathVariable UUID orderId,
            HttpServletRequest httpRequest) {
        String userId = httpRequest.getHeader("X-User-Id");

        if (userId == null || userId.isBlank()) {
            return buildError(HttpStatus.BAD_REQUEST, "MISSING_USER_ID",
                    "X-User-Id header is required");
        }

        try {
            OrderResponse order = orderService.getOrderById(orderId, userId);
            if (order == null) {
                return buildError(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND",
                        "Order not found");
            }
            return ResponseEntity.ok(order);
        } catch (IllegalStateException e) {
            if (e.getMessage().contains("not found")) {
                return buildError(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", e.getMessage());
            }
            return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                    "Failed to fetch order: " + e.getMessage());
        }
    }

    private ResponseEntity<Map<String, Object>> buildError(
            HttpStatus status, String errorCode, String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error_code", errorCode);
        error.put("message", message);
        error.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.status(status).body(error);
    }
}