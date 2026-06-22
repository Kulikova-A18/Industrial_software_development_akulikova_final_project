package com.orbitamarket.payments.repository;

import com.orbitamarket.payments.model.InboxEvent;
import com.orbitamarket.payments.model.InboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InboxRepository extends JpaRepository<InboxEvent, UUID> {
    boolean existsByOrderId(UUID orderId);
    List<InboxEvent> findByStatus(InboxStatus status);
}