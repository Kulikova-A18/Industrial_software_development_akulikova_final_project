package com.orbitamarket.gateway;

import com.orbitamarket.gateway.config.GatewayConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.session.DefaultWebSessionManager;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class SimpleGatewayTest {

    @Autowired
    private RouteLocator routeLocator;

    @Test
    public void testRoutesAreConfigured() {
        var routes = routeLocator.getRoutes().collectList().block();
        assertNotNull(routes);
        assertFalse(routes.isEmpty());
        
        // Проверяем, что все маршруты имеют правильные URI
        for (Route route : routes) {
            URI uri = route.getUri();
            assertNotNull(uri);
            System.out.println("Route: " + route.getId() + " -> " + uri);
        }
    }

    @Test
    public void testRoutePredicates() {
        var routes = routeLocator.getRoutes().collectList().block();
        assertNotNull(routes);
        
        // Проверяем, что маршрут payments настроен правильно
        boolean hasPayments = routes.stream()
                .anyMatch(route -> route.getId().equals("payments") && 
                         route.getPredicate() != null);
        assertTrue(hasPayments, "Payments route with predicate should exist");
        
        // Проверяем, что маршрут orders настроен правильно
        boolean hasOrders = routes.stream()
                .anyMatch(route -> route.getId().equals("orders") && 
                         route.getPredicate() != null);
        assertTrue(hasOrders, "Orders route with predicate should exist");
    }

    @Test
    public void testRouteUris() {
        var routes = routeLocator.getRoutes().collectList().block();
        assertNotNull(routes);
        
        // Проверяем URI для маршрута payments
        routes.stream()
                .filter(route -> route.getId().equals("payments"))
                .findFirst()
                .ifPresent(route -> {
                    assertEquals("http://payments-service:8081", route.getUri().toString());
                });
        
        // Проверяем URI для маршрута orders
        routes.stream()
                .filter(route -> route.getId().equals("orders"))
                .findFirst()
                .ifPresent(route -> {
                    assertEquals("http://orders-service:8082", route.getUri().toString());
                });
    }
}