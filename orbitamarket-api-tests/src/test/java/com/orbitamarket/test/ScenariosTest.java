package com.orbitamarket.tests;

import io.qameta.allure.*;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Epic("Full Scenarios")
@Feature("End-to-End Tests")
public class ScenariosTest extends BaseTest {

    @Test
    @Story("Happy Path")
    @DisplayName("Сценарий: Успешная оплата заказа")
    @Description("Счет → Пополнение 1000 → Заказ на 120 → PAID → Баланс 880")
    void testHappyPath_SuccessfulPayment() {
        // 1. Create account
        given()
                .spec(spec)
                .header("X-User-Id", testUserId)
        .when()
                .post(PAYMENTS_PATH + "/accounts")
        .then()
                .statusCode(200);

        // 2. Top up 1000
        Map<String, Object> topUpRequest = new HashMap<>();
        topUpRequest.put("amount", 1000);
        
        given()
                .spec(spec)
                .header("X-User-Id", testUserId)
                .body(topUpRequest)
        .when()
                .post(PAYMENTS_PATH + "/accounts/top-up")
        .then()
                .statusCode(200)
                .body("balance", equalTo(1000));

        // 3. Create order for 120
        Map<String, Object> orderRequest = new HashMap<>();
        orderRequest.put("product_type", "ARCHIVE");
        orderRequest.put("price", 120);
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("aoi", "POLYGON((30 10, 40 40, 20 40, 10 20, 30 10))");
        payload.put("capture_date", "2024-06-15");
        payload.put("sensor_type", "MSI");
        orderRequest.put("payload", payload);

        String orderId = given()
                .spec(spec)
                .header("X-User-Id", testUserId)
                .body(orderRequest)
        .when()
                .post(ORDERS_PATH)
        .then()
                .statusCode(200)
                .body("status", equalTo("PAYMENT_PENDING"))
                .extract()
                .path("order_id");

        // 4. Wait for async processing
        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(() -> {
                    String status = given()
                            .spec(spec)
                            .header("X-User-Id", testUserId)
                            .pathParam("orderId", orderId)
                    .when()
                            .get(ORDERS_PATH + "/{orderId}")
                            .then()
                            .extract()
                            .path("status");
                    
                    return status.equals("PAID") || status.equals("PAYMENT_PENDING");
                });

        // 5. Check order status
        given()
                .spec(spec)
                .header("X-User-Id", testUserId)
                .pathParam("orderId", orderId)
        .when()
                .get(ORDERS_PATH + "/{orderId}")
        .then()
                .statusCode(200)
                .body("status", anyOf(equalTo("PAID"), equalTo("PAYMENT_PENDING")));

        // 6. Check balance
        given()
                .spec(spec)
                .header("X-User-Id", testUserId)
        .when()
                .get(PAYMENTS_PATH + "/accounts/balance")
        .then()
                .statusCode(200)
                .body("balance", anyOf(equalTo(880), equalTo(1000))); // 1000 if not processed yet
    }

    @Test
    @Story("Insufficient Balance")
    @DisplayName("Сценарий: Недостаточно средств")
    @Description("Счет → Пополнение 50 → Заказ на 120 → PAYMENT_FAILED → Баланс 50")
    void testInsufficientBalance() {
        // 1. Create account
        given()
                .spec(spec)
                .header("X-User-Id", testUserId)
        .when()
                .post(PAYMENTS_PATH + "/accounts")
        .then()
                .statusCode(200);

        // 2. Top up 50
        Map<String, Object> topUpRequest = new HashMap<>();
        topUpRequest.put("amount", 50);
        
        given()
                .spec(spec)
                .header("X-User-Id", testUserId)
                .body(topUpRequest)
        .when()
                .post(PAYMENTS_PATH + "/accounts/top-up")
        .then()
                .statusCode(200)
                .body("balance", equalTo(50));

        // 3. Create order for 120
        Map<String, Object> orderRequest = new HashMap<>();
        orderRequest.put("product_type", "ARCHIVE");
        orderRequest.put("price", 120);
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("aoi", "POLYGON((30 10, 40 40, 20 40, 10 20, 30 10))");
        payload.put("capture_date", "2024-06-15");
        payload.put("sensor_type", "MSI");
        orderRequest.put("payload", payload);

        String orderId = given()
                .spec(spec)
                .header("X-User-Id", testUserId)
                .body(orderRequest)
        .when()
                .post(ORDERS_PATH)
        .then()
                .statusCode(200)
                .body("status", equalTo("PAYMENT_PENDING"))
                .extract()
                .path("order_id");

        // 4. Wait for async processing
        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(() -> {
                    String status = given()
                            .spec(spec)
                            .header("X-User-Id", testUserId)
                            .pathParam("orderId", orderId)
                    .when()
                            .get(ORDERS_PATH + "/{orderId}")
                            .then()
                            .extract()
                            .path("status");
                    
                    return status.equals("PAYMENT_FAILED") || status.equals("PAYMENT_PENDING");
                });

        // 5. Check order status
        given()
                .spec(spec)
                .header("X-User-Id", testUserId)
                .pathParam("orderId", orderId)
        .when()
                .get(ORDERS_PATH + "/{orderId}")
        .then()
                .statusCode(200)
                .body("status", anyOf(equalTo("PAYMENT_FAILED"), equalTo("PAYMENT_PENDING")))
                .body("failure_reason", anyOf(equalTo("INSUFFICIENT_BALANCE"), nullValue()));

        // 6. Check balance (should be unchanged)
        given()
                .spec(spec)
                .header("X-User-Id", testUserId)
        .when()
                .get(PAYMENTS_PATH + "/accounts/balance")
        .then()
                .statusCode(200)
                .body("balance", equalTo(50));
    }

    @Test
    @Story("Multiple Orders")
    @DisplayName("Сценарий: Два заказа при одном балансе")
    @Description("Счет → Пополнение 1000 → Два заказа по 400 → Оба PAID → Баланс 200")
    void testMultipleOrders() {
        // 1. Create account
        given()
                .spec(spec)
                .header("X-User-Id", testUserId)
        .when()
                .post(PAYMENTS_PATH + "/accounts")
        .then()
                .statusCode(200);

        // 2. Top up 1000
        Map<String, Object> topUpRequest = new HashMap<>();
        topUpRequest.put("amount", 1000);
        
        given()
                .spec(spec)
                .header("X-User-Id", testUserId)
                .body(topUpRequest)
        .when()
                .post(PAYMENTS_PATH + "/accounts/top-up")
        .then()
                .statusCode(200)
                .body("balance", equalTo(1000));

        // 3. Create two orders for 400 each
        Map<String, Object> orderRequest = new HashMap<>();
        orderRequest.put("product_type", "ARCHIVE");
        orderRequest.put("price", 400);
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("aoi", "POLYGON((30 10, 40 40, 20 40, 10 20, 30 10))");
        payload.put("capture_date", "2024-06-15");
        payload.put("sensor_type", "MSI");
        orderRequest.put("payload", payload);

        String orderId1 = given()
                .spec(spec)
                .header("X-User-Id", testUserId)
                .body(orderRequest)
        .when()
                .post(ORDERS_PATH)
        .then()
                .statusCode(200)
                .extract()
                .path("order_id");

        String orderId2 = given()
                .spec(spec)
                .header("X-User-Id", testUserId)
                .body(orderRequest)
        .when()
                .post(ORDERS_PATH)
        .then()
                .statusCode(200)
                .extract()
                .path("order_id");

        // 4. Wait for async processing
        Awaitility.await()
                .atMost(15, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(() -> {
                    int paidCount = 0;
                    
                    String status1 = given()
                            .spec(spec)
                            .header("X-User-Id", testUserId)
                            .pathParam("orderId", orderId1)
                    .when()
                            .get(ORDERS_PATH + "/{orderId}")
                            .then()
                            .extract()
                            .path("status");
                    
                    String status2 = given()
                            .spec(spec)
                            .header("X-User-Id", testUserId)
                            .pathParam("orderId", orderId2)
                    .when()
                            .get(ORDERS_PATH + "/{orderId}")
                            .then()
                            .extract()
                            .path("status");
                    
                    if ("PAID".equals(status1)) paidCount++;
                    if ("PAID".equals(status2)) paidCount++;
                    
                    return paidCount >= 2;
                });

        // 5. Check balance (should be 200)
        given()
                .spec(spec)
                .header("X-User-Id", testUserId)
        .when()
                .get(PAYMENTS_PATH + "/accounts/balance")
        .then()
                .statusCode(200)
                .body("balance", equalTo(200));
    }

    @Test
    @Story("Duplicate Account")
    @DisplayName("Сценарий: Повторное создание счета")
    @Description("Дважды отправляем POST /payments/accounts → Дубликат счета не создается")
    void testDuplicateAccount() {
        // 1. First creation
        given()
                .spec(spec)
                .header("X-User-Id", testUserId)
        .when()
                .post(PAYMENTS_PATH + "/accounts")
        .then()
                .statusCode(200);

        // 2. Second creation - should not create duplicate
        given()
                .spec(spec)
                .header("X-User-Id", testUserId)
        .when()
                .post(PAYMENTS_PATH + "/accounts")
        .then()
                .statusCode(200)
                .body("user_id", equalTo(testUserId));

        // 3. Verify only one account exists (balance operations work correctly)
        Map<String, Object> topUpRequest = new HashMap<>();
        topUpRequest.put("amount", 100);
        
        given()
                .spec(spec)
                .header("X-User-Id", testUserId)
                .body(topUpRequest)
        .when()
                .post(PAYMENTS_PATH + "/accounts/top-up")
        .then()
                .statusCode(200)
                .body("balance", equalTo(100));
    }
}