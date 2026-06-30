package com.orbitamarket.tests;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.util.UUID;

public abstract class BaseTest {

    protected static final String GATEWAY_URL = "http://localhost:8080";
    protected static final String PAYMENTS_PATH = "/api/v1/payments";
    protected static final String ORDERS_PATH = "/api/v1/orders";
    
    protected static RequestSpecification spec;
    protected String testUserId;

    @BeforeAll
    public static void setupSpec() {
        RestAssured.baseURI = GATEWAY_URL;
        RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());
        
        spec = new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .build();
    }

    @BeforeEach
    public void setupUser() {
        testUserId = "test-user-" + UUID.randomUUID().toString().substring(0, 8);
    }

    protected String getUserId() {
        return testUserId;
    }
}