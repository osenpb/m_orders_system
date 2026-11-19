package com.osen.msvc_product.outbox;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Publica los eventos pendientes del outbox en stock-events (polling).
 * Entrega AL MENOS UNA VEZ: msvc-order es idempotente por eventId (inbox).
 * El tipo del evento viaja en la cabecera eventType.
 */
@Component
@Lazy(false)
public class OutboxPublisher {

    public static final String STOCK_EVENTS_TOPIC = "stock-events";
    public static final String EVENT_TYPE_HEADER = "eventType";

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final TransactionTemplate tx;

    public OutboxPublisher(OutboxRepository outboxRepository, KafkaTemplate<String, String> kafkaTemplate,
            TransactionTemplate tx) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.tx = tx;
    }

    @Scheduled(fixedDelayString = "${outbox.poll-interval-ms:1000}")
    public void publishPending() {
        List<OutboxEvent> pending = outboxRepository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();
        for (OutboxEvent event : pending) {
            try {
                ProducerRecord<String, String> record = new ProducerRecord<>(STOCK_EVENTS_TOPIC,
                        event.getAggregateId(), event.getPayload());
                record.headers().add(EVENT_TYPE_HEADER, event.getType().getBytes(StandardCharsets.UTF_8));
                kafkaTemplate.send(record).get(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                log.warn("No se pudo publicar el evento {} ({}); se reintentara", event.getId(), e.toString());
                return;
            }
            tx.executeWithoutResult(status -> outboxRepository.findById(event.getId())
                    .ifPresent(OutboxEvent::markPublished));
        }
    }
}
