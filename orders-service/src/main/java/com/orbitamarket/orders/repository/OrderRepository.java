package com.orbitamarket.orders.repository;

import com.orbitamarket.orders.model.Order;
import com.orbitamarket.orders.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByUserId(String userId);
    List<Order> findByStatus(OrderStatus status);
    long countByUserId(String userId);
}