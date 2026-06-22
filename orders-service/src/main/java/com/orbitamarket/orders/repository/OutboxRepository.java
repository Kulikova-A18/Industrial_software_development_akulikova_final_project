package com.orbitamarket.orders.repository;

import com.orbitamarket.orders.model.OutboxEvent;
import com.orbitamarket.orders.model.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {
    List<OutboxEvent> findByStatusOrderByCreatedAt(OutboxStatus status);
}