package com.orbitamarket.orders.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class OrderResponse {
    @JsonProperty("order_id")
    private UUID orderId;

    private String status;

    @JsonProperty("product_type")
    private String productType;

    private Integer price;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    private String failureReason;
}