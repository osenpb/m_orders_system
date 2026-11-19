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
import com.osen.msvc_order.events.StockRejectedEvent;
import com.osen.msvc_order.events.StockReservedEvent;
import com.osen.msvc_order.services.OrderSagaService;

@Component
@Lazy(false)
public class StockEventsListener {

    private static final Logger log = LoggerFactory.getLogger(StockEventsListener.class);

    private final OrderSagaService orderSagaService;
    private final ObjectMapper objectMapper;

    public StockEventsListener(OrderSagaService orderSagaService, ObjectMapper objectMapper) {
        this.orderSagaService = orderSagaService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "stock-events", groupId = "msvc-order")
    public void onStockEvent(ConsumerRecord<String, String> record) throws JsonProcessingException {
        Header typeHeader = record.headers().lastHeader("eventType");
        String type = typeHeader == null ? "" : new String(typeHeader.value(), StandardCharsets.UTF_8);
        switch (type) {
            // el stock reservado ya no confirma el pedido: falta el pago (payment-events)
            case StockReservedEvent.TYPE -> log.debug("Stock reservado para el pedido {}, esperando el pago",
                    record.key());
            case StockRejectedEvent.TYPE -> {
                StockRejectedEvent e = objectMapper.readValue(record.value(), StockRejectedEvent.class);
                orderSagaService.onStockRejected(e.eventId(), e.orderId(), e.reason());
            }
            default -> log.warn("Evento desconocido '{}' en stock-events, se ignora", type);
        }
    }
}
