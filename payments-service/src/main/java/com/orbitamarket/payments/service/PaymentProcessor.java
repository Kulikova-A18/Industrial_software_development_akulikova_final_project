package com.orbitamarket.payments.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orbitamarket.payments.dto.OrderPaymentCompleted;
import com.orbitamarket.payments.dto.OrderPaymentFailed;
import com.orbitamarket.payments.dto.PaymentRequest;
import com.orbitamarket.payments.model.InboxEvent;
import com.orbitamarket.payments.model.InboxStatus;
import com.orbitamarket.payments.repository.InboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentProcessor {

    private final AccountService accountService;
    private final InboxRepository inboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String REQUEST_TOPIC = "order_payment_requests";
    private static final String COMPLETED_TOPIC = "payment_completed_events";
    private static final String FAILED_TOPIC = "payment_failed_events";

    @KafkaListener(topics = REQUEST_TOPIC, groupId = "payments-group")
    @Transactional
    public void processPayment(String message) {
        try {
            PaymentRequest request = objectMapper.readValue(message, PaymentRequest.class);
            log.info("Received payment request: order_id={}, user_id={}, amount={}",
                    request.getOrderId(), request.getUserId(), request.getAmount());

            // Check inbox for idempotency
            if (inboxRepository.existsByOrderId(request.getOrderId())) {
                log.info("Order {} already processed, skipping", request.getOrderId());
                return;
            }

            // Save inbox event
            InboxEvent inboxEvent = InboxEvent.builder()
                    .eventId(request.getEventId())
                    .orderId(request.getOrderId())
                    .userId(request.getUserId())
                    .amount(request.getAmount())
                    .status(InboxStatus.PENDING)
                    .rawEvent(message)
                    .createdAt(LocalDateTime.now())
                    .build();
            inboxRepository.save(inboxEvent);

            // Attempt to debit
            boolean success = accountService.debitBalance(
                    request.getUserId(),
                    request.getAmount(),
                    request.getOrderId().toString()
            );

            if (success) {
                // Update inbox
                inboxEvent.setStatus(InboxStatus.PROCESSED);
                inboxEvent.setProcessedAt(LocalDateTime.now());
                inboxEvent.setProcessingResult("SUCCESS");
                inboxRepository.save(inboxEvent);

                // Get updated balance
                int newBalance = accountService.getAccount(request.getUserId()).getBalance();

                // Send completion event
                OrderPaymentCompleted completed = OrderPaymentCompleted.builder()
                        .eventId(UUID.randomUUID())
                        .orderId(request.getOrderId())
                        .userId(request.getUserId())
                        .amount(request.getAmount())
                        .newBalance(newBalance)
                        .build();

                kafkaTemplate.send(COMPLETED_TOPIC,
                        request.getUserId(),
                        objectMapper.writeValueAsString(completed));

                log.info("Payment completed: order_id={}, new_balance={}",
                        request.getOrderId(), newBalance);

            } else {
                // Update inbox with failure
                inboxEvent.setStatus(InboxStatus.FAILED);
                inboxEvent.setProcessedAt(LocalDateTime.now());
                inboxEvent.setProcessingResult("INSUFFICIENT_BALANCE");
                inboxRepository.save(inboxEvent);

                // Send failure event
                OrderPaymentFailed failed = OrderPaymentFailed.builder()
                        .eventId(UUID.randomUUID())
                        .orderId(request.getOrderId())
                        .userId(request.getUserId())
                        .reason("INSUFFICIENT_BALANCE")
                        .build();

                kafkaTemplate.send(FAILED_TOPIC,
                        request.getUserId(),
                        objectMapper.writeValueAsString(failed));

                log.info("Payment failed: order_id={}, reason=INSUFFICIENT_BALANCE",
                        request.getOrderId());
            }

        } catch (Exception e) {
            log.error("Error processing payment: {}", e.getMessage(), e);
        }
    }
}