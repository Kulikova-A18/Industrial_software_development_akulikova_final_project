package com.orbitamarket.payments.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BalanceResponse {
    @JsonProperty("user_id")
    private String userId;

    private Integer balance;

    private String currency;

    public BalanceResponse(String userId, Integer balance, String currency) {
        this.userId = userId;
        this.balance = balance;
        this.currency = currency;
    }
}