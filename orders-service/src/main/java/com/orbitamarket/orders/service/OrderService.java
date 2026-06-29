package com.orbitamarket.orders.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orbitamarket.orders.dto.OrderPaymentRequested;
import com.orbitamarket.orders.dto.OrderResponse;
import com.orbitamarket.orders.model.Order;
import com.orbitamarket.orders.model.OrderStatus;
import com.orbitamarket.orders.model.OutboxEvent;
import com.orbitamarket.orders.model.OutboxStatus;
import com.orbitamarket.orders.model.ProductType;
import com.orbitamarket.orders.repository.OrderRepository;
import com.orbitamarket.orders.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public OrderResponse createOrder(String userId, String productTypeStr,
                                      Integer price, Map<String, Object> payload) {
        ProductType productType;
        try {
            productType = ProductType.valueOf(productTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("UNKNOWN_PRODUCT_TYPE");
        }

        // Validate payload based on product type
        validatePayload(productType, payload);

        UUID orderId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        // Сериализация payload с обработкой исключения
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.error("Error serializing payload for order: {}", orderId, e);
            throw new RuntimeException("Failed to serialize payload", e);
        }

        Order order = Order.builder()
                .id(orderId)
                .userId(userId)
                .productType(productType)
                .price(price)
                .payload(payloadJson)
                .status(OrderStatus.CREATED)
                .createdAt(now)
                .updatedAt(now)
                .build();

        order = orderRepository.save(order);

        // Create outbox event
        OrderPaymentRequested event = OrderPaymentRequested.builder()
                .eventId(UUID.randomUUID())
                .orderId(orderId)
                .userId(userId)
                .amount(price)
                .occurredAt(now)
                .build();

        // Сериализация события с обработкой исключения
        String eventData;
        try {
            eventData = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            log.error("Error serializing outbox event for order: {}", orderId, e);
            throw new RuntimeException("Failed to serialize outbox event", e);
        }

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .userId(userId)
                .amount(price)
                .eventType("OrderPaymentRequested")
                .eventData(eventData)
                .createdAt(now)
                .status(OutboxStatus.PENDING)
                .build();

        outboxRepository.save(outboxEvent);

        // Update order status to PAYMENT_PENDING
        order.setStatus(OrderStatus.PAYMENT_PENDING);
        order.setUpdatedAt(now);
        orderRepository.save(order);

        log.info("Order created: {} with status PAYMENT_PENDING", orderId);

        return mapToResponse(order);
    }

    private void validatePayload(ProductType productType, Map<String, Object> payload) {
        switch (productType) {
            case ARCHIVE:
                if (!payload.containsKey("aoi") || !payload.containsKey("capture_date") ||
                        !payload.containsKey("sensor_type")) {
                    throw new IllegalArgumentException("Missing required fields for ARCHIVE: " +
                            "aoi, capture_date, sensor_type");
                }
                break;
            case TASKING:
                if (!payload.containsKey("aoi") || !payload.containsKey("time_window") ||
                        !payload.containsKey("sensor_type")) {
                    throw new IllegalArgumentException("Missing required fields for TASKING: " +
                            "aoi, time_window, sensor_type");
                }
                break;
            case MONITORING:
                if (!payload.containsKey("aoi") || !payload.containsKey("cadence") ||
                        !payload.containsKey("duration_days")) {
                    throw new IllegalArgumentException("Missing required fields for MONITORING: " +
                            "aoi, cadence, duration_days");
                }
                break;
        }
    }

    public List<OrderResponse> getOrdersByUserId(String userId) {
        List<Order> orders = orderRepository.findByUserId(userId);
        return orders.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public OrderResponse getOrderById(UUID orderId, String userId) {
        Order order = orderRepository.findById(orderId)
                .orElse(null);

        if (order == null) {
            throw new IllegalStateException("Order not found");
        }

        if (!order.getUserId().equals(userId)) {
            throw new IllegalStateException("Order not found");
        }

        return mapToResponse(order);
    }

    @Transactional
    public void updateOrderStatus(UUID orderId, OrderStatus status, String failureReason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("Order not found"));

        order.setStatus(status);
        order.setFailureReason(failureReason);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        log.info("Order {} updated to status: {}, reason: {}", orderId, status, failureReason);
    }

    @Transactional
    public void updateOrderToPaid(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("Order not found"));

        if (order.getStatus() != OrderStatus.PAYMENT_PENDING &&
                order.getStatus() != OrderStatus.CREATED) {
            log.warn("Order {} already in final state: {}, skipping", orderId, order.getStatus());
            return;
        }

        order.setStatus(OrderStatus.PAID);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        log.info("Order {} marked as PAID", orderId);
    }

    private OrderResponse mapToResponse(Order order) {
        return OrderResponse.builder()
                .orderId(order.getId())
                .status(order.getStatus().name())
                .productType(order.getProductType().name())
                .price(order.getPrice())
                .createdAt(order.getCreatedAt())
                .failureReason(order.getFailureReason())
                .build();
    }
}