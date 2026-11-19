package com.osen.msvc_order.messaging;

import java.nio.charset.StandardCharsets;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.osen.msvc_order.config.KafkaErrorHandlingConfig;

/**
 * Hace visible lo que cae en el DLT. No reprocesa nada: un humano decide si
 * corregir y reenviar el mensaje. Aqui solo se deja constancia en el log.
 */
@Component
@Lazy(false)
public class DeadLetterListener {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterListener.class);

    // stock-events.DLT es compartido: aqui tambien caen los fallos de msvc-payment (ver el grupo en el log)
    @KafkaListener(topics = { KafkaErrorHandlingConfig.STOCK_EVENTS_DLT,
            KafkaErrorHandlingConfig.PAYMENT_EVENTS_DLT }, groupId = "msvc-order-dlt")
    public void onDeadLetter(ConsumerRecord<String, String> record) {
        log.error("Mensaje en {} (clave={}, origen={}, grupo={}): {} | payload={}", record.topic(), record.key(),
                header(record, KafkaHeaders.DLT_ORIGINAL_TOPIC),
                header(record, KafkaHeaders.DLT_ORIGINAL_CONSUMER_GROUP),
                header(record, KafkaHeaders.DLT_EXCEPTION_MESSAGE), record.value());
    }

    private static String header(ConsumerRecord<?, ?> record, String name) {
        Header h = record.headers().lastHeader(name);
        return h == null ? "-" : new String(h.value(), StandardCharsets.UTF_8);
    }
}
