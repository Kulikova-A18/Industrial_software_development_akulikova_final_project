package com.orbitamarket.payments.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orbitamarket.payments.dto.PaymentRequest;
import com.orbitamarket.payments.model.InboxEvent;
import com.orbitamarket.payments.model.InboxStatus;
import com.orbitamarket.payments.repository.InboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentProcessorTest {

    @Mock
    private AccountService accountService;

    @Mock
    private InboxRepository inboxRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private PaymentProcessor paymentProcessor;

    private PaymentRequest paymentRequest;
    private String testMessage;
    private UUID orderId;
    private String userId;

    @BeforeEach
    void setUp() throws Exception {
        orderId = UUID.randomUUID();
        userId = "user-123";

        paymentRequest = PaymentRequest.builder()
                .eventId(UUID.randomUUID())
                .orderId(orderId)
                .userId(userId)
                .amount(100)
                .occurredAt(LocalDateTime.now().toString())
                .build();

        testMessage = "{\"event_id\":\"" + paymentRequest.getEventId() + "\",\"order_id\":\"" + orderId + "\",\"user_id\":\"" + userId + "\",\"amount\":100}";

        when(objectMapper.readValue(testMessage, PaymentRequest.class))
                .thenReturn(paymentRequest);
    }

    @Test
    void processPayment_Success() throws Exception {
        when(inboxRepository.existsByOrderId(any(UUID.class))).thenReturn(false);
        when(inboxRepository.save(any(InboxEvent.class))).thenReturn(new InboxEvent());
        when(accountService.debitBalance(anyString(), anyInt(), anyString())).thenReturn(true);
        com.orbitamarket.payments.model.Account account = com.orbitamarket.payments.model.Account.builder()
                .userId(userId)
                .balance(50)
                .build();
        when(accountService.getAccount(anyString())).thenReturn(account);

        paymentProcessor.processPayment(testMessage);

        verify(inboxRepository).existsByOrderId(orderId);
        verify(accountService).debitBalance(userId, 100, orderId.toString());
        verify(kafkaTemplate).send(anyString(), anyString(), anyString());
    }

    @Test
    void processPayment_DuplicateOrder_SkipsProcessing() throws Exception {
        when(inboxRepository.existsByOrderId(any(UUID.class))).thenReturn(true);

        paymentProcessor.processPayment(testMessage);

        verify(inboxRepository, never()).save(any(InboxEvent.class));
        verify(accountService, never()).debitBalance(anyString(), anyInt(), anyString());
    }

    @Test
    void processPayment_InsufficientBalance_SendsFailedEvent() throws Exception {
        when(inboxRepository.existsByOrderId(any(UUID.class))).thenReturn(false);
        when(inboxRepository.save(any(InboxEvent.class))).thenReturn(new InboxEvent());
        when(accountService.debitBalance(anyString(), anyInt(), anyString())).thenReturn(false);

        paymentProcessor.processPayment(testMessage);

        verify(kafkaTemplate).send(eq("payment_failed_events"), anyString(), anyString());
    }

    @Test
    void processPayment_Exception_LogsError() throws Exception {
        when(inboxRepository.existsByOrderId(any(UUID.class))).thenThrow(new RuntimeException("Database error"));

        paymentProcessor.processPayment(testMessage);

        verify(accountService, never()).debitBalance(anyString(), anyInt(), anyString());
    }
}