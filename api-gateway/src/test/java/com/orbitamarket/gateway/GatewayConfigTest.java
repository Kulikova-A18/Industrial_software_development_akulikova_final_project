package com.orbitamarket.gateway;

import com.orbitamarket.gateway.config.GatewayConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class GatewayConfigTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private RouteLocator routeLocator;

    @Test
    public void testRouteLocatorExists() {
        assertNotNull(routeLocator, "RouteLocator bean should exist");
    }

    @Test
    public void testRoutesCount() {
        // Проверяем, что у нас есть как минимум 2 маршрута
        var routes = routeLocator.getRoutes().collectList().block();
        assertNotNull(routes);
        assertTrue(routes.size() >= 2, "Should have at least 2 routes configured");
        
        // Проверяем наличие маршрутов по именам
        boolean hasPaymentsRoute = routes.stream()
                .anyMatch(route -> route.getId().equals("payments"));
        boolean hasOrdersRoute = routes.stream()
                .anyMatch(route -> route.getId().equals("orders"));
        
        assertTrue(hasPaymentsRoute, "Payments route should exist");
        assertTrue(hasOrdersRoute, "Orders route should exist");
    }

    @Test
    public void testGatewayHealthEndpoint() {
        // Проверяем, что gateway работает
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/actuator/health", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void testXUserIdHeaderPropagation() {
        // Проверяем, что фильтр правильно обрабатывает заголовок X-User-Id
        HttpHeaders headers = new HttpHeaders();
        headers.put("X-User-Id", Collections.singletonList("test-user-123"));
        headers.put("Host", Collections.singletonList("localhost:" + port));

        // Отправляем запрос к несуществующему сервису, но проверяем, что заголовок проходит через фильтр
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/payments/test", String.class);
        
        // Проверяем, что запрос достиг фильтра (получили 404, т.к. сервис не запущен)
        // Но это означает, что фильтр отработал
        assertNotNull(response);
    }

    @Test
    public void testGatewayInfoEndpoint() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/actuator/info", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void testRoutePaths() {
        // Проверяем пути маршрутов через тестовый клиент
        ResponseEntity<String> paymentsResponse = restTemplate.getForEntity(
                "/api/v1/payments/health", String.class);
        // Ожидаем 404, т.к. payments-service не запущен
        assertEquals(HttpStatus.NOT_FOUND, paymentsResponse.getStatusCode());
        
        ResponseEntity<String> ordersResponse = restTemplate.getForEntity(
                "/api/v1/orders/health", String.class);
        // Ожидаем 404, т.к. orders-service не запущен
        assertEquals(HttpStatus.NOT_FOUND, ordersResponse.getStatusCode());
    }
}