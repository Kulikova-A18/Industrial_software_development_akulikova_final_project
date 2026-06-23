package com.orbitamarket.payments.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class OrderPaymentCompleted {
    @JsonProperty("event_id")
    private UUID eventId;

    @JsonProperty("order_id")
    private UUID orderId;

    @JsonProperty("user_id")
    private String userId;

    private Integer amount;

    @JsonProperty("new_balance")
    private Integer newBalance;
}