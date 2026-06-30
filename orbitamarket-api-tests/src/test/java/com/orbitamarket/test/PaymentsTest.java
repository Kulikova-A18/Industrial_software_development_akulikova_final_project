package com.orbitamarket.tests;

import io.qameta.allure.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Epic("Payments Service")
@Feature("Account Management")
public class PaymentsTest extends BaseTest {

    @Test
    @Story("Create Account")
    @DisplayName("POST /api/v1/payments/accounts - Успешное создание счета")
    @Description("Создание счета для нового пользователя должно возвращать баланс 0")
    void testCreateAccount_Success() {
        given()
                .spec(spec)
                .header("X-User-Id", testUserId)
        .when()
                .post(PAYMENTS_PATH + "/accounts")
        .then()
                .statusCode(200)
                .body("user_id", equalTo(testUserId))
                .body("balance", equalTo(0))
                .body("currency", equalTo("geocredits"));
    }

    @Test
    @Story("Create Account")
    @DisplayName("POST /api/v1/payments/accounts - Повторное создание счета (идемпотентность)")
    @Description("Повторный запрос для того же user_id должен вернуть существующий счет без дублирования")
    void testCreateAccount_Idempotent() {
        // First creation
        given()
                .spec(spec)
                .header("X-User-Id", testUserId)
        .when()
                .post(PAYMENTS_PATH + "/accounts")
        .then()
                .statusCode(200);

        // Second creation - should be idempotent
        given()
                .spec(spec)
                .header("X-User-Id", testUserId)
        .when()
                .post(PAYMENTS_PATH + "/accounts")
        .then()
                .statusCode(200)
                .body("user_id", equalTo(testUserId))
                .body("balance", equalTo(0));
    }

    @Test
    @Story("Create Account")
    @DisplayName("POST /api/v1/payments/accounts - Отсутствует X-User-Id")
    @Description("Запрос без заголовка X-User-Id должен возвращать ошибку")
    void testCreateAccount_MissingUserId() {
        given()
                .spec(spec)
        .when()
                .post(PAYMENTS_PATH + "/accounts")
        .then()
                .statusCode(400)
                .body("error_code", equalTo("MISSING_USER_ID"));
    }

    @Test
    @Story("Top Up Account")
    @DisplayName("POST /api/v1/payments/accounts/top-up - Успешное пополнение баланса")
    @Description("Пополнение баланса на положительную сумму должно увеличить баланс")
    void testTopUp_Success() {
        // Create account first
        given()
                .spec(spec)
                .header("X-User-Id", testUserId)
        .when()
                .post(PAYMENTS_PATH + "/accounts")
        .then()
                .statusCode(200);

        // Top up
        Map<String, Object> request = new HashMap<>();
        request.put("amount", 1000);

        given()
                .spec(spec)
                .header("X-User-Id", testUserId)
                .body(request)
        .when()
                .post(PAYMENTS_PATH + "/accounts/top-up")
        .then()
                .statusCode(200)
                .body("user_id", equalTo(testUserId))
                .body("balance", equalTo(1000))
                .body("currency", equalTo("geocredits"));
    }

    @Test
    @Story("Top Up Account")
    @DisplayName("POST /api/v1/payments/accounts/top-up - Некорректная сумма (0)")
    @Description("Пополнение на сумму 0 должно возвращать ошибку INVALID_AMOUNT")
    void testTopUp_InvalidAmount() {
        // Create account first
        given()
                .spec(spec)
                .header("X-User-Id", testUserId)
        .when()
                .post(PAYMENTS_PATH + "/accounts")
        .then()
                .statusCode(200);

        Map<String, Object> request = new HashMap<>();
        request.put("amount", 0);

        given()
                .spec(spec)
                .header("X-User-Id", testUserId)
                .body(request)
        .when()
                .post(PAYMENTS_PATH + "/accounts/top-up")
        .then()
                .statusCode(400)
                .body("error_code", equalTo("INVALID_AMOUNT"));
    }

    @Test
    @Story("Top Up Account")
    @DisplayName("POST /api/v1/payments/accounts/top-up - Негативная сумма")
    @Description("Пополнение на отрицательную сумму должно возвращать ошибку INVALID_AMOUNT")
    void testTopUp_NegativeAmount() {
        // Create account first
        given()
                .spec(spec)
                .header("X-User-Id", testUserId)
        .when()
                .post(PAYMENTS_PATH + "/accounts")
        .then()
                .statusCode(200);

        Map<String, Object> request = new HashMap<>();
        request.put("amount", -100);

        given()
                .spec(spec)
                .header("X-User-Id", testUserId)
                .body(request)
        .when()
                .post(PAYMENTS_PATH + "/accounts/top-up")
        .then()
                .statusCode(400)
                .body("error_code", equalTo("INVALID_AMOUNT"));
    }

    @Test
    @Story("Top Up Account")
    @DisplayName("POST /api/v1/payments/accounts/top-up - Счет не найден")
    @Description("Пополнение для несуществующего пользователя должно возвращать ошибку ACCOUNT_NOT_FOUND")
    void testTopUp_AccountNotFound() {
        Map<String, Object> request = new HashMap<>();
        request.put("amount", 100);

        given()
                .spec(spec)
                .header("X-User-Id", "non-existent-user")
                .body(request)
        .when()
                .post(PAYMENTS_PATH + "/accounts/top-up")
        .then()
                .statusCode(404)
                .body("error_code", equalTo("ACCOUNT_NOT_FOUND"));
    }

    @Test
    @Story("Get Balance")
    @DisplayName("GET /api/v1/payments/accounts/balance - Успешное получение баланса")
    @Description("Получение баланса для существующего пользователя")
    void testGetBalance_Success() {
        // Create account and top up
        given()
                .spec(spec)
                .header("X-User-Id", testUserId)
                .post(PAYMENTS_PATH + "/accounts")
        .then()
                .statusCode(200);

        Map<String, Object> topUpRequest = new HashMap<>();
        topUpRequest.put("amount", 500);
        given()
                .spec(spec)
                .header("X-User-Id", testUserId)
                .body(topUpRequest)
                .post(PAYMENTS_PATH + "/accounts/top-up")
        .then()
                .statusCode(200);

        // Get balance
        given()
                .spec(spec)
                .header("X-User-Id", testUserId)
        .when()
                .get(PAYMENTS_PATH + "/accounts/balance")
        .then()
                .statusCode(200)
                .body("user_id", equalTo(testUserId))
                .body("balance", equalTo(500))
                .body("currency", equalTo("geocredits"));
    }

    @Test
    @Story("Get Balance")
    @DisplayName("GET /api/v1/payments/accounts/balance - Счет не найден")
    @Description("Получение баланса для несуществующего пользователя должно возвращать ошибку")
    void testGetBalance_AccountNotFound() {
        given()
                .spec(spec)
                .header("X-User-Id", "non-existent-user")
        .when()
                .get(PAYMENTS_PATH + "/accounts/balance")
        .then()
                .statusCode(404)
                .body("error_code", equalTo("ACCOUNT_NOT_FOUND"));
    }

    @Test
    @Story("Get Balance")
    @DisplayName("GET /api/v1/payments/accounts/balance - Отсутствует X-User-Id")
    @Description("Запрос баланса без заголовка X-User-Id должен возвращать ошибку")
    void testGetBalance_MissingUserId() {
        given()
                .spec(spec)
        .when()
                .get(PAYMENTS_PATH + "/accounts/balance")
        .then()
                .statusCode(400)
                .body("error_code", equalTo("MISSING_USER_ID"));
    }
}