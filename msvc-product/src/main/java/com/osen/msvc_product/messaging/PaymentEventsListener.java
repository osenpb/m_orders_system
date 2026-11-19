package com.osen.msvc_product.messaging;

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
import com.osen.msvc_product.events.PaymentFailedEvent;
import com.osen.msvc_product.service.StockCompensationService;

/** Escucha el resultado del pago: solo PaymentFailed requiere accion (compensar). */
@Component
@Lazy(false)
public class PaymentEventsListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventsListener.class);

    private final StockCompensationService stockCompensationService;
    private final ObjectMapper objectMapper;

    public PaymentEventsListener(StockCompensationService stockCompensationService, ObjectMapper objectMapper) {
        this.stockCompensationService = stockCompensationService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "payment-events", groupId = "msvc-product")
    public void onPaymentEvent(ConsumerRecord<String, String> record) throws JsonProcessingException {
        Header typeHeader = record.headers().lastHeader("eventType");
        String type = typeHeader == null ? "" : new String(typeHeader.value(), StandardCharsets.UTF_8);
        if (PaymentFailedEvent.TYPE.equals(type)) {
            stockCompensationService.releaseStock(objectMapper.readValue(record.value(), PaymentFailedEvent.class));
        } else {
            log.debug("Evento {} ignorado por product", type);
        }
    }
}
