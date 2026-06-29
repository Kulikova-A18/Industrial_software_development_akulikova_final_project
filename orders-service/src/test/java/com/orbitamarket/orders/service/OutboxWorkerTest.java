package com.orbitamarket.orders.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orbitamarket.orders.model.OutboxEvent;
import com.orbitamarket.orders.model.OutboxStatus;
import com.orbitamarket.orders.repository.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxWorkerTest {

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OutboxWorker outboxWorker;

    private UUID eventId;
    private UUID orderId;
    private String userId;
    private OutboxEvent outboxEvent;

    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        userId = "test-user-123";
        
        outboxEvent = OutboxEvent.builder()
                .id(eventId)
                .orderId(orderId)
                .userId(userId)
                .amount(1000)
                .eventType("OrderPaymentRequested")
                .eventData("{\"test\":\"data\"}")
                .createdAt(LocalDateTime.now())
                .status(OutboxStatus.PENDING)
                .build();
    }

    @Test
    void processOutboxEvents_WithPendingEvents_ShouldSendToKafka() {
        // Given
        List<OutboxEvent> pendingEvents = Arrays.asList(outboxEvent);
        when(outboxRepository.findByStatusOrderByCreatedAt(OutboxStatus.PENDING))
                .thenReturn(pendingEvents);
        when(outboxRepository.save(any(OutboxEvent.class))).thenReturn(outboxEvent);

        // When
        outboxWorker.processOutboxEvents();

        // Then
        verify(kafkaTemplate, times(1)).send(
                eq("order_payment_requests"),
                eq(userId),
                eq(outboxEvent.getEventData())
        );
        verify(outboxRepository, times(1)).save(outboxEvent);
        assertEquals(OutboxStatus.SENT, outboxEvent.getStatus());
        assertNotNull(outboxEvent.getProcessedAt());
    }

    @Test
    void processOutboxEvents_WithNoPendingEvents_ShouldDoNothing() {
        // Given
        when(outboxRepository.findByStatusOrderByCreatedAt(OutboxStatus.PENDING))
                .thenReturn(Collections.emptyList());

        // When
        outboxWorker.processOutboxEvents();

        // Then
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
        verify(outboxRepository, never()).save(any());
    }

    @Test
    void processOutboxEvents_WithMultipleEvents_ShouldProcessAll() {
        // Given
        OutboxEvent event2 = OutboxEvent.builder()
                .id(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .userId("another-user")
                .amount(2000)
                .eventType("OrderPaymentRequested")
                .eventData("{\"test2\":\"data2\"}")
                .createdAt(LocalDateTime.now())
                .status(OutboxStatus.PENDING)
                .build();

        List<OutboxEvent> pendingEvents = Arrays.asList(outboxEvent, event2);
        when(outboxRepository.findByStatusOrderByCreatedAt(OutboxStatus.PENDING))
                .thenReturn(pendingEvents);
        when(outboxRepository.save(any(OutboxEvent.class)))
                .thenReturn(outboxEvent)
                .thenReturn(event2);

        // When
        outboxWorker.processOutboxEvents();

        // Then
        verify(kafkaTemplate, times(2)).send(
                eq("order_payment_requests"),
                anyString(),
                anyString()
        );
        verify(outboxRepository, times(2)).save(any(OutboxEvent.class));
    }

    @Test
    void processOutboxEvents_WithKafkaFailure_ShouldMarkAsFailed() {
        // Given
        List<OutboxEvent> pendingEvents = Arrays.asList(outboxEvent);
        when(outboxRepository.findByStatusOrderByCreatedAt(OutboxStatus.PENDING))
                .thenReturn(pendingEvents);
        doThrow(new RuntimeException("Kafka error"))
                .when(kafkaTemplate).send(anyString(), anyString(), anyString());
        when(outboxRepository.save(any(OutboxEvent.class))).thenReturn(outboxEvent);

        // When
        outboxWorker.processOutboxEvents();

        // Then
        verify(kafkaTemplate, times(1)).send(
                eq("order_payment_requests"),
                eq(userId),
                eq(outboxEvent.getEventData())
        );
        verify(outboxRepository, times(1)).save(outboxEvent);
        assertEquals(OutboxStatus.FAILED, outboxEvent.getStatus());
        assertNotNull(outboxEvent.getProcessedAt());
    }

    @Test
    void processOutboxEvents_ShouldBeScheduled() throws Exception {
        // Verify that the method has @Scheduled annotation
        var method = OutboxWorker.class.getDeclaredMethod("processOutboxEvents");
        var scheduledAnnotation = method.getAnnotation(org.springframework.scheduling.annotation.Scheduled.class);
        assert scheduledAnnotation != null;
        assertEquals("5000", scheduledAnnotation.fixedDelay());
    }
}