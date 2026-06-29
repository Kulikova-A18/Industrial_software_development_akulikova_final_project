package com.orbitamarket.payments.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.UUID;

@Data
public class PaymentRequest {
    @JsonProperty("event_id")
    private UUID eventId;

    @JsonProperty("order_id")
    private UUID orderId;

    @JsonProperty("user_id")
    private String userId;

    private Integer amount;

    @JsonProperty("occurred_at")
    private String occurredAt;
}