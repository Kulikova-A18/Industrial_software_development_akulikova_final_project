package com.orbitamarket.orders.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orbitamarket.orders.dto.OrderPaymentRequested;
import com.orbitamarket.orders.model.OutboxEvent;
import com.orbitamarket.orders.model.OutboxStatus;
import com.orbitamarket.orders.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class OutboxWorker {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String TOPIC = "order_payment_requests";

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void processOutboxEvents() {
        List<OutboxEvent> pendingEvents = outboxRepository
                .findByStatusOrderByCreatedAt(OutboxStatus.PENDING);

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("Processing {} pending outbox events", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            try {
                // Send to Kafka
                kafkaTemplate.send(TOPIC, event.getUserId(), event.getEventData());

                event.setStatus(OutboxStatus.SENT);
                event.setProcessedAt(LocalDateTime.now());
                outboxRepository.save(event);

                log.info("Outbox event {} sent to Kafka", event.getId());
            } catch (Exception e) {
                log.error("Failed to send outbox event {}: {}", event.getId(), e.getMessage());
                event.setStatus(OutboxStatus.FAILED);
                event.setProcessedAt(LocalDateTime.now());
                outboxRepository.save(event);
            }
        }
    }
}