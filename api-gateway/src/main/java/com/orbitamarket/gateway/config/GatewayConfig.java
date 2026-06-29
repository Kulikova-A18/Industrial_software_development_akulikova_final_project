package com.orbitamarket.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("payments", r -> r
                        .path("/api/v1/payments/**")
                        .filters(f -> f
                                .addRequestHeader("X-User-Id", "${header.X-User-Id}")
                        )
                        .uri("http://payments-service:8081"))
                .route("orders", r -> r
                        .path("/api/v1/orders/**")
                        .filters(f -> f
                                .addRequestHeader("X-User-Id", "${header.X-User-Id}")
                        )
                        .uri("http://orders-service:8082"))
                .build();
    }
}