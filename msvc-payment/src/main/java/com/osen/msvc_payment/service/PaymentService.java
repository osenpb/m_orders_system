package com.osen.msvc_payment.service;

import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.osen.msvc_payment.entity.Payment;
import com.osen.msvc_payment.entity.PaymentStatus;
import com.osen.msvc_payment.events.PaymentCompletedEvent;
import com.osen.msvc_payment.events.PaymentFailedEvent;
import com.osen.msvc_payment.events.StockReservedEvent;
import com.osen.msvc_payment.inbox.ProcessedEvent;
import com.osen.msvc_payment.inbox.ProcessedEventRepository;
import com.osen.msvc_payment.outbox.OutboxEvent;
import com.osen.msvc_payment.outbox.OutboxRepository;
import com.osen.msvc_payment.repository.PaymentRepository;

/**
 * Paso de la SAGA en payment: cobra el pedido cuyo stock ya se reservo.
 *
 * No hay pasarela real: el cobro se simula con una regla determinista (se
 * rechaza todo importe mayor que payment.max-amount) para poder provocar el
 * camino de fallo y ver las compensaciones. Inbox, pago y outbox van en una
 * sola transaccion, asi el evento solo se procesa una vez.
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final OutboxRepository outboxRepository;
    private final TransactionTemplate tx;
    private final ObjectMapper objectMapper;
    private final double maxAmount;

    public PaymentService(PaymentRepository paymentRepository, ProcessedEventRepository processedEventRepository,
            OutboxRepository outboxRepository, TransactionTemplate tx, ObjectMapper objectMapper,
            @Value("${payment.max-amount:1000}") double maxAmount) {
        this.paymentRepository = paymentRepository;
        this.processedEventRepository = processedEventRepository;
        this.outboxRepository = outboxRepository;
        this.tx = tx;
        this.objectMapper = objectMapper;
        this.maxAmount = maxAmount;
    }

    public void handle(StockReservedEvent event) {
        if (processedEventRepository.existsById(event.eventId())) {
            log.info("Evento {} ya procesado, se ignora", event.eventId());
            return;
        }
        try {
            tx.executeWithoutResult(status -> {
                processedEventRepository.saveAndFlush(new ProcessedEvent(event.eventId()));
                boolean approved = event.amount() != null && event.amount() <= maxAmount;
                if (approved) {
                    paymentRepository.save(new Payment(event.orderId(), event.userId(), event.amount(),
                            PaymentStatus.COMPLETED, null));
                    outboxRepository.save(toOutbox(PaymentCompletedEvent.TYPE, event.orderId(),
                            new PaymentCompletedEvent(UUID.randomUUID(), event.orderId(), event.amount(),
                                    Instant.now())));
                    log.info("Pago completado del pedido {} por {}", event.orderId(), event.amount());
                } else {
                    String reason = "Importe " + event.amount() + " supera el limite de " + maxAmount;
                    paymentRepository.save(new Payment(event.orderId(), event.userId(), event.amount(),
                            PaymentStatus.FAILED, reason));
                    outboxRepository.save(toOutbox(PaymentFailedEvent.TYPE, event.orderId(),
                            new PaymentFailedEvent(UUID.randomUUID(), event.orderId(), reason, Instant.now())));
                    log.info("Pago fallido del pedido {}: {}", event.orderId(), reason);
                }
            });
        } catch (DataIntegrityViolationException e) {
            // eventId repetido (inbox) u orderId ya cobrado: es un duplicado
            log.info("Evento {} duplicado detectado, se ignora", event.eventId());
        }
    }

    private OutboxEvent toOutbox(String type, Long orderId, Object payload) {
        try {
            return new OutboxEvent(UUID.randomUUID(), String.valueOf(orderId), type,
                    objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar " + type, e);
        }
    }
}
