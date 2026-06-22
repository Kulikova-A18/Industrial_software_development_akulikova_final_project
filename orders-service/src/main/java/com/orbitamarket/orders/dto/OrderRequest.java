package com.orbitamarket.orders.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
public class OrderRequest {
    @JsonProperty("product_type")
    private String productType;

    private Integer price;

    private Map<String, Object> payload;
}