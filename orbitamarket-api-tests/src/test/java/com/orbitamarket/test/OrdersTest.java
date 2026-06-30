package com.orbitamarket.tests;

import io.qameta.allure.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Epic("Orders Service")
@Feature("Order Management")
public class OrdersTest extends BaseTest {

    private Map<String, Object> createArchivePayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("aoi", "POLYGON((30 10, 40 40, 20 40, 10 20, 30 10))");
        payload.put("capture_date", "2024-06-15");
        payload.put("sensor_type", "MSI");
        return payload;
    }

    private Map<String, Object> createOrderRequest(String productType, int price) {
        Map<String, Object> request = new HashMap<>();
        request.put("product_type", productType);
        request.put("price", price);
        request.put("payload", createArchivePayload());
        return request;
    }

    private String createOrderAndGetId() {
        Map<String, Object> request = createOrderRequest("ARCHIVE", 100);
        
        return given()
                .spec(spec)
                .header("X-User-Id", testUserId)
                .body(request)
        .when()
                .post(ORDERS_PATH)
        .then()
                .statusCode(200)
                .extract()
                .path("order_id");
    }

    @Test
    @Story("Create Order")
    @DisplayName("POST /api/v1/orders - Успешное создание заказа ARCHIVE")
    @Description("Создание заказа типа ARCHIVE должно возвращать статус PAYMENT_PENDING")
    void testCreateOrder_Archive_Success() {
        Map<String, Object> request = createOrderRequest("ARCHIVE", 120);

        given()
                .spec(spec)
                .header("X-User-Id", testUserId)
                .body(request)
        .when()
                .post(ORDERS_PATH)
        .then()
                .statusCode(200)
                .body("order_id", notNullValue())
                .body("status", equalTo("PAYMENT_PENDING"))
                .body("product_type", equalTo("ARCHIVE"))
                .body("price", equalTo(120))
                .body("created_at", notNullValue());
    }

    @Test
    @Story("Create Order")
    @DisplayName("POST /api/v1/orders - Ошибка: некорректная цена (0)")
    @Description("Создание заказа с ценой 0 должно возвращать INVALID_PRICE")
    void testCreateOrder_InvalidPrice() {
        Map<String, Object> request = createOrderRequest("ARCHIVE", 0);

        given()
                .spec(spec)
                .header("X-User-Id", testUserId)
                .body(request)
        .when()
                .post(ORDERS_PATH)
        .then()
                .statusCode(400)
                .body("error_code", equalTo("INVALID_PRICE"));
    }

    @Test
    @Story("Create Order")
    @DisplayName("POST /api/v1/orders - Ошибка: отрицательная цена")
    @Description("Создание заказа с отрицательной ценой должно возвращать INVALID_PRICE")
    void testCreateOrder_NegativePrice() {
        Map<String, Object> request = createOrderRequest("ARCHIVE", -50);

        given()
                .spec(spec)
                .header("X-User-Id", testUserId)
                .body(request)
        .when()
                .post(ORDERS_PATH)
        .then()
                .statusCode(400)
                .body("error_code", equalTo("INVALID_PRICE"));
    }

    @Test
    @Story("Create Order")
    @DisplayName("POST /api/v1/orders - Ошибка: неизвестный тип продукта")
    @Description("Создание заказа с неизвестным product_type должно возвращать UNKNOWN_PRODUCT_TYPE")
    void testCreateOrder_UnknownProductType() {
        Map<String, Object> request = createOrderRequest("UNKNOWN", 100);

        given()
                .spec(spec)
                .header("X-User-Id", testUserId)
                .body(request)
        .when()
                .post(ORDERS_PATH)
        .then()
                .statusCode(400)
                .body("error_code", equalTo("UNKNOWN_PRODUCT_TYPE"));
    }

    @Test
    @Story("Create Order")
    @DisplayName("POST /api/v1/orders - Ошибка: отсутствует X-User-Id")
    @Description("Создание заказа без заголовка X-User-Id должно возвращать MISSING_USER_ID")
    void testCreateOrder_MissingUserId() {
        Map<String, Object> request = createOrderRequest("ARCHIVE", 100);

        given()
                .spec(spec)
                .body(request)
        .when()
                .post(ORDERS_PATH)
        .then()
                .statusCode(400)
                .body("error_code", equalTo("MISSING_USER_ID"));
    }

    @Test
    @Story("Create Order")
    @DisplayName("POST /api/v1/orders - Ошибка: отсутствуют обязательные поля payload")
    @Description("Создание заказа без обязательных полей payload должно возвращать INVALID_PAYLOAD")
    void testCreateOrder_InvalidPayload() {
        Map<String, Object> request = new HashMap<>();
        request.put("product_type", "ARCHIVE");
        request.put("price", 100);
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("aoi", "POLYGON((30 10, 40 40, 20 40, 10 20, 30 10))");
        // Missing capture_date and sensor_type
        request.put("payload", payload);

        given()
                .spec(spec)
                .header("X-User-Id", testUserId)
                .body(request)
        .when()
                .post(ORDERS_PATH)
        .then()
                .statusCode(400)
                .body("error_code", equalTo("INVALID_PAYLOAD"));
    }

    @Test
    @Story("Get Orders")
    @DisplayName("GET /api/v1/orders - Успешное получение списка заказов")
    @Description("Получение списка заказов для пользователя")
    void testGetOrders_Success() {
        // Create a few orders
        createOrderAndGetId();
        createOrderAndGetId();

        given()
                .spec(spec)
                .header("X-User-Id", testUserId)
        .when()
                .get(ORDERS_PATH)
        .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(2));
    }

    @Test
    @Story("Get Orders")
    @DisplayName("GET /api/v1/orders - Отсутствует X-User-Id")
    @Description("Получение списка заказов без заголовка X-User-Id должно возвращать MISSING_USER_ID")
    void testGetOrders_MissingUserId() {
        given()
                .spec(spec)
        .when()
                .get(ORDERS_PATH)
        .then()
                .statusCode(400)
                .body("error_code", equalTo("MISSING_USER_ID"));
    }

    @Test
    @Story("Get Order By ID")
    @DisplayName("GET /api/v1/orders/{order_id} - Успешное получение заказа по ID")
    @Description("Получение заказа по ID должно возвращать детали заказа")
    void testGetOrderById_Success() {
        String orderId = createOrderAndGetId();

        given()
                .spec(spec)
                .header("X-User-Id", testUserId)
                .pathParam("orderId", orderId)
        .when()
                .get(ORDERS_PATH + "/{orderId}")
        .then()
                .statusCode(200)
                .body("order_id", equalTo(orderId))
                .body("user_id", equalTo(testUserId))
                .body("status", equalTo("PAYMENT_PENDING"))
                .body("product_type", equalTo("ARCHIVE"));
    }

    @Test
    @Story("Get Order By ID")
    @DisplayName("GET /api/v1/orders/{order_id} - Ошибка: заказ не найден")
    @Description("Получение несуществующего заказа должно возвращать ORDER_NOT_FOUND")
    void testGetOrderById_NotFound() {
        String nonExistentId = "00000000-0000-0000-0000-000000000000";

        given()
                .spec(spec)
                .header("X-User-Id", testUserId)
                .pathParam("orderId", nonExistentId)
        .when()
                .get(ORDERS_PATH + "/{orderId}")
        .then()
                .statusCode(404)
                .body("error_code", equalTo("ORDER_NOT_FOUND"));
    }

    @Test
    @Story("Get Order By ID")
    @DisplayName("GET /api/v1/orders/{order_id} - Отсутствует X-User-Id")
    @Description("Получение заказа по ID без заголовка X-User-Id должно возвращать MISSING_USER_ID")
    void testGetOrderById_MissingUserId() {
        given()
                .spec(spec)
                .pathParam("orderId", "00000000-0000-0000-0000-000000000000")
        .when()
                .get(ORDERS_PATH + "/{orderId}")
        .then()
                .statusCode(400)
                .body("error_code", equalTo("MISSING_USER_ID"));
    }
}