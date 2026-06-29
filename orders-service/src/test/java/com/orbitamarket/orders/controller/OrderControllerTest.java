package com.orbitamarket.orders.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orbitamarket.orders.dto.OrderRequest;
import com.orbitamarket.orders.dto.OrderResponse;
import com.orbitamarket.orders.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    private UUID testOrderId;
    private String testUserId;
    private OrderResponse testResponse;

    @BeforeEach
    void setUp() {
        testOrderId = UUID.randomUUID();
        testUserId = "test-user-123";
        testResponse = OrderResponse.builder()
                .orderId(testOrderId)
                .status("PAYMENT_PENDING")
                .productType("ARCHIVE")
                .price(1000)
                .createdAt(LocalDateTime.now())
                .failureReason(null)
                .build();
    }

    @Test
    void createOrder_WithValidRequest_ShouldReturnOk() throws Exception {
        // Given
        OrderRequest request = new OrderRequest();
        request.setProductType("ARCHIVE");
        request.setPrice(1000);
        Map<String, Object> payload = new HashMap<>();
        payload.put("aoi", "test-aoi");
        payload.put("capture_date", "2024-01-01");
        payload.put("sensor_type", "optical");
        request.setPayload(payload);

        when(orderService.createOrder(anyString(), anyString(), anyInt(), anyMap()))
                .thenReturn(testResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                .header("X-User-Id", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order_id").value(testOrderId.toString()))
                .andExpect(jsonPath("$.status").value("PAYMENT_PENDING"))
                .andExpect(jsonPath("$.product_type").value("ARCHIVE"))
                .andExpect(jsonPath("$.price").value(1000));
    }

    @Test
    void createOrder_WithoutUserId_ShouldReturnBadRequest() throws Exception {
        // Given
        OrderRequest request = new OrderRequest();
        request.setProductType("ARCHIVE");
        request.setPrice(1000);
        request.setPayload(new HashMap<>());

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("MISSING_USER_ID"));
    }

    @Test
    void createOrder_WithInvalidPrice_ShouldReturnBadRequest() throws Exception {
        // Given
        OrderRequest request = new OrderRequest();
        request.setProductType("ARCHIVE");
        request.setPrice(-100);
        request.setPayload(new HashMap<>());

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                .header("X-User-Id", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("INVALID_PRICE"));
    }

    @Test
    void createOrder_WithEmptyProductType_ShouldReturnBadRequest() throws Exception {
        // Given
        OrderRequest request = new OrderRequest();
        request.setProductType("");
        request.setPrice(1000);
        request.setPayload(new HashMap<>());

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                .header("X-User-Id", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("INVALID_PAYLOAD"));
    }

    @Test
    void getOrders_WithValidUserId_ShouldReturnOk() throws Exception {
        // Given
        when(orderService.getOrdersByUserId(testUserId))
                .thenReturn(Arrays.asList(testResponse));

        // When & Then
        mockMvc.perform(get("/api/v1/orders")
                .header("X-User-Id", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].order_id").value(testOrderId.toString()))
                .andExpect(jsonPath("$[0].status").value("PAYMENT_PENDING"));
    }

    @Test
    void getOrders_WithoutUserId_ShouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("MISSING_USER_ID"));
    }

    @Test
    void getOrder_WithValidId_ShouldReturnOk() throws Exception {
        // Given
        when(orderService.getOrderById(testOrderId, testUserId))
                .thenReturn(testResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/orders/{orderId}", testOrderId)
                .header("X-User-Id", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order_id").value(testOrderId.toString()))
                .andExpect(jsonPath("$.status").value("PAYMENT_PENDING"));
    }

    @Test
    void getOrder_WithoutUserId_ShouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/orders/{orderId}", testOrderId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("MISSING_USER_ID"));
    }
}