package com.orbitamarket.payments.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AccountResponse {
    @JsonProperty("user_id")
    private String userId;

    private Integer balance;

    private String currency;
}