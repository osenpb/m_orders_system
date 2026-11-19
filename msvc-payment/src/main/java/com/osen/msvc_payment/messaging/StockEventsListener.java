package com.osen.msvc_payment.messaging;

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
import com.osen.msvc_payment.events.StockReservedEvent;
import com.osen.msvc_payment.service.PaymentService;

/** Escucha stock-events: solo StockReserved dispara el cobro, StockRejected no le incumbe. */
@Component
@Lazy(false)
public class StockEventsListener {

    private static final Logger log = LoggerFactory.getLogger(StockEventsListener.class);

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    public StockEventsListener(PaymentService paymentService, ObjectMapper objectMapper) {
        this.paymentService = paymentService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "stock-events", groupId = "msvc-payment")
    public void onStockEvent(ConsumerRecord<String, String> record) throws JsonProcessingException {
        Header typeHeader = record.headers().lastHeader("eventType");
        String type = typeHeader == null ? "" : new String(typeHeader.value(), StandardCharsets.UTF_8);
        if (StockReservedEvent.TYPE.equals(type)) {
            paymentService.handle(objectMapper.readValue(record.value(), StockReservedEvent.class));
        } else {
            log.debug("Evento {} ignorado por payment", type);
        }
    }
}
