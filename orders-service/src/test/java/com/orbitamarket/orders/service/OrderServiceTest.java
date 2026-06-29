package com.orbitamarket.orders.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orbitamarket.orders.dto.OrderResponse;
import com.orbitamarket.orders.model.Order;
import com.orbitamarket.orders.model.OrderStatus;
import com.orbitamarket.orders.model.OutboxEvent;
import com.orbitamarket.orders.model.ProductType;
import com.orbitamarket.orders.repository.OrderRepository;
import com.orbitamarket.orders.repository.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OrderService orderService;

    private String userId;
    private UUID orderId;
    private Order order;
    private HashMap<String, Object> payload;

    @BeforeEach
    void setUp() throws Exception {
        userId = "test-user-123";
        orderId = UUID.randomUUID();
        
        payload = new HashMap<>();
        payload.put("aoi", "test-aoi");
        payload.put("capture_date", "2024-01-01");
        payload.put("sensor_type", "optical");

        order = Order.builder()
                .id(orderId)
                .userId(userId)
                .productType(ProductType.ARCHIVE)
                .price(1000)
                .payload("{\"aoi\":\"test-aoi\",\"capture_date\":\"2024-01-01\",\"sensor_type\":\"optical\"}")
                .status(OrderStatus.CREATED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(objectMapper.writeValueAsString(any())).thenReturn("{\"test\":\"data\"}");
    }

    @Test
    void createOrder_WithValidData_ShouldCreateOrder() {
        // Given
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(outboxRepository.save(any(OutboxEvent.class))).thenReturn(new OutboxEvent());

        // When
        OrderResponse response = orderService.createOrder(
                userId, "ARCHIVE", 1000, payload);

        // Then
        assertNotNull(response);
        assertEquals(orderId, response.getOrderId());
        assertEquals("PAYMENT_PENDING", response.getStatus());
        assertEquals("ARCHIVE", response.getProductType());
        assertEquals(1000, response.getPrice());
        
        verify(orderRepository, times(2)).save(any(Order.class));
        verify(outboxRepository, times(1)).save(any(OutboxEvent.class));
    }

    @Test
    void createOrder_WithInvalidProductType_ShouldThrowException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> orderService.createOrder(userId, "INVALID_TYPE", 1000, payload)
        );
        assertEquals("UNKNOWN_PRODUCT_TYPE", exception.getMessage());
    }

    @Test
    void createOrder_WithInvalidArchivePayload_ShouldThrowException() {
        // Given
        HashMap<String, Object> invalidPayload = new HashMap<>();
        invalidPayload.put("aoi", "test-aoi");
        // Missing capture_date and sensor_type

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> orderService.createOrder(userId, "ARCHIVE", 1000, invalidPayload)
        );
        assertTrue(exception.getMessage().contains("Missing required fields for ARCHIVE"));
    }

    @Test
    void createOrder_WithInvalidTaskingPayload_ShouldThrowException() {
        // Given
        HashMap<String, Object> invalidPayload = new HashMap<>();
        invalidPayload.put("aoi", "test-aoi");
        // Missing time_window and sensor_type

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> orderService.createOrder(userId, "TASKING", 1000, invalidPayload)
        );
        assertTrue(exception.getMessage().contains("Missing required fields for TASKING"));
    }

    @Test
    void createOrder_WithInvalidMonitoringPayload_ShouldThrowException() {
        // Given
        HashMap<String, Object> invalidPayload = new HashMap<>();
        invalidPayload.put("aoi", "test-aoi");
        // Missing cadence and duration_days

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> orderService.createOrder(userId, "MONITORING", 1000, invalidPayload)
        );
        assertTrue(exception.getMessage().contains("Missing required fields for MONITORING"));
    }

    @Test
    void getOrdersByUserId_ShouldReturnOrders() {
        // Given
        List<Order> orders = Arrays.asList(order);
        when(orderRepository.findByUserId(userId)).thenReturn(orders);

        // When
        List<OrderResponse> responses = orderService.getOrdersByUserId(userId);

        // Then
        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(orderId, responses.get(0).getOrderId());
        assertEquals("ARCHIVE", responses.get(0).getProductType());
        verify(orderRepository, times(1)).findByUserId(userId);
    }

    @Test
    void getOrdersByUserId_WithNoOrders_ShouldReturnEmptyList() {
        // Given
        when(orderRepository.findByUserId(userId)).thenReturn(Arrays.asList());

        // When
        List<OrderResponse> responses = orderService.getOrdersByUserId(userId);

        // Then
        assertNotNull(responses);
        assertTrue(responses.isEmpty());
        verify(orderRepository, times(1)).findByUserId(userId);
    }

    @Test
    void getOrderById_WithValidId_ShouldReturnOrder() {
        // Given
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // When
        OrderResponse response = orderService.getOrderById(orderId, userId);

        // Then
        assertNotNull(response);
        assertEquals(orderId, response.getOrderId());
        assertEquals("ARCHIVE", response.getProductType());
        verify(orderRepository, times(1)).findById(orderId);
    }

    @Test
    void getOrderById_WithInvalidId_ShouldThrowException() {
        // Given
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // When & Then
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> orderService.getOrderById(orderId, userId)
        );
        assertEquals("Order not found", exception.getMessage());
    }

    @Test
    void getOrderById_WithWrongUserId_ShouldThrowException() {
        // Given
        String wrongUserId = "wrong-user";
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // When & Then
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> orderService.getOrderById(orderId, wrongUserId)
        );
        assertEquals("Order not found", exception.getMessage());
    }

    @Test
    void updateOrderStatus_ShouldUpdateStatus() {
        // Given
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // When
        orderService.updateOrderStatus(orderId, OrderStatus.PAID, null);

        // Then
        assertEquals(OrderStatus.PAID, order.getStatus());
        assertNotNull(order.getUpdatedAt());
        verify(orderRepository, times(1)).save(order);
    }

    @Test
    void updateOrderStatus_WithInvalidId_ShouldThrowException() {
        // Given
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // When & Then
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> orderService.updateOrderStatus(orderId, OrderStatus.PAID, null)
        );
        assertEquals("Order not found", exception.getMessage());
    }

    @Test
    void updateOrderToPaid_ShouldUpdateToPaid() {
        // Given
        order.setStatus(OrderStatus.PAYMENT_PENDING);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // When
        orderService.updateOrderToPaid(orderId);

        // Then
        assertEquals(OrderStatus.PAID, order.getStatus());
        assertNotNull(order.getUpdatedAt());
        verify(orderRepository, times(1)).save(order);
    }

    @Test
    void updateOrderToPaid_OrderAlreadyPaid_ShouldSkip() {
        // Given
        order.setStatus(OrderStatus.PAID);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // When
        orderService.updateOrderToPaid(orderId);

        // Then
        assertEquals(OrderStatus.PAID, order.getStatus());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void updateOrderToPaid_OrderInFinalState_ShouldSkip() {
        // Given
        order.setStatus(OrderStatus.PAYMENT_FAILED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // When
        orderService.updateOrderToPaid(orderId);

        // Then
        assertEquals(OrderStatus.PAYMENT_FAILED, order.getStatus());
        verify(orderRepository, never()).save(any());
    }
}