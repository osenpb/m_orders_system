package com.osen.msvc_product.messaging;

import java.nio.charset.StandardCharsets;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.osen.msvc_product.events.OrderCreatedEvent;
import com.osen.msvc_product.service.StockReservationService;

@Component
@Lazy(false)
public class OrderEventsListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventsListener.class);

    private final StockReservationService stockReservationService;
    private final ObjectMapper objectMapper;

    public OrderEventsListener(StockReservationService stockReservationService, ObjectMapper objectMapper) {
        this.stockReservationService = stockReservationService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "order-events", groupId = "msvc-product")
    public void onOrderEvent(ConsumerRecord<String, String> record) throws JsonProcessingException {
        Header typeHeader = record.headers().lastHeader("eventType");
        String type = typeHeader == null ? OrderCreatedEvent.TYPE
                : new String(typeHeader.value(), StandardCharsets.UTF_8);
        if (!OrderCreatedEvent.TYPE.equals(type)) {
            log.debug("Evento {} ignorado por product", type);
            return;
        }
        stockReservationService.handle(objectMapper.readValue(record.value(), OrderCreatedEvent.class));
    }
}
