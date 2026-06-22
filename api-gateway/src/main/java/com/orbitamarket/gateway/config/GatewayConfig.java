package com.orbitamarket.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("payments", r -> r
                        .path("/api/v1/payments/**")
                        .filters(f -> f
                                .filter((exchange, chain) -> {
                                    String userId = exchange.getRequest().getHeaders()
                                            .getFirst("X-User-Id");
                                    if (userId != null) {
                                        exchange = exchange.mutate()
                                                .request(exchange.getRequest().mutate()
                                                        .header("X-User-Id", userId)
                                                        .build())
                                                .build();
                                    }
                                    return chain.filter(exchange);
                                })
                                .stripPrefix(1)
                        )
                        .uri("http://payments-service:8081"))
                .route("orders", r -> r
                        .path("/api/v1/orders/**")
                        .filters(f -> f
                                .filter((exchange, chain) -> {
                                    String userId = exchange.getRequest().getHeaders()
                                            .getFirst("X-User-Id");
                                    if (userId != null) {
                                        exchange = exchange.mutate()
                                                .request(exchange.getRequest().mutate()
                                                        .header("X-User-Id", userId)
                                                        .build())
                                                .build();
                                    }
                                    return chain.filter(exchange);
                                })
                                .stripPrefix(1)
                        )
                        .uri("http://orders-service:8082"))
                .build();
    }
}