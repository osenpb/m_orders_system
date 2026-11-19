package com.osen.msvc_order.messaging;

import java.nio.charset.StandardCharsets;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.osen.msvc_order.events.PaymentCompletedEvent;
import com.osen.msvc_order.events.PaymentFailedEvent;
import com.osen.msvc_order.services.OrderSagaService;

@Component
@Lazy(false)
public class PaymentEventsListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventsListener.class);

    private final OrderSagaService orderSagaService;
    private final ObjectMapper objectMapper;

    public PaymentEventsListener(OrderSagaService orderSagaService, ObjectMapper objectMapper) {
        this.orderSagaService = orderSagaService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "payment-events", groupId = "msvc-order")
    public void onPaymentEvent(ConsumerRecord<String, String> record) throws JsonProcessingException {
        Header typeHeader = record.headers().lastHeader("eventType");
        String type = typeHeader == null ? "" : new String(typeHeader.value(), StandardCharsets.UTF_8);
        switch (type) {
            case PaymentCompletedEvent.TYPE -> {
                PaymentCompletedEvent e = objectMapper.readValue(record.value(), PaymentCompletedEvent.class);
                orderSagaService.onPaymentCompleted(e.eventId(), e.orderId());
            }
            case PaymentFailedEvent.TYPE -> {
                PaymentFailedEvent e = objectMapper.readValue(record.value(), PaymentFailedEvent.class);
                orderSagaService.onPaymentFailed(e.eventId(), e.orderId(), e.reason());
            }
            default -> log.warn("Evento desconocido '{}' en payment-events, se ignora", type);
        }
    }
}
