package com.orbitamarket.orders.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class OrderPaymentFailed {
    @JsonProperty("event_id")
    private UUID eventId;

    @JsonProperty("order_id")
    private UUID orderId;

    @JsonProperty("user_id")
    private String userId;

    private String reason;
}