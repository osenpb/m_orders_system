package com.osen.msvc_product.service;

import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.osen.msvc_product.entity.StockReservation;
import com.osen.msvc_product.repository.StockReservationRepository;
import com.osen.msvc_product.events.OrderCreatedEvent;
import com.osen.msvc_product.events.StockRejectedEvent;
import com.osen.msvc_product.events.StockReservedEvent;
import com.osen.msvc_product.inbox.ProcessedEvent;
import com.osen.msvc_product.inbox.ProcessedEventRepository;
import com.osen.msvc_product.outbox.OutboxEvent;
import com.osen.msvc_product.outbox.OutboxRepository;
import com.osen.msvc_product.repository.ProductRepository;

/**
 * Paso de la SAGA en product: reserva el stock del pedido (todo o nada).
 *
 * Idempotente: el eventId se registra en la inbox en la MISMA transaccion que
 * el descuento de stock, asi un reenvio del mismo evento no descuenta dos veces.
 * Cada resultado (StockReserved / StockRejected) se escribe en el outbox, nunca
 * se envia directamente a Kafka.
 */
@Service
public class StockReservationService {

    private static final Logger log = LoggerFactory.getLogger(StockReservationService.class);

    private final ProductRepository productRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final OutboxRepository outboxRepository;
    private final StockReservationRepository stockReservationRepository;
    private final TransactionTemplate tx;
    private final ObjectMapper objectMapper;

    public StockReservationService(ProductRepository productRepository,
            ProcessedEventRepository processedEventRepository, OutboxRepository outboxRepository,
            StockReservationRepository stockReservationRepository, TransactionTemplate tx,
            ObjectMapper objectMapper) {
        this.stockReservationRepository = stockReservationRepository;
        this.productRepository = productRepository;
        this.processedEventRepository = processedEventRepository;
        this.outboxRepository = outboxRepository;
        this.tx = tx;
        this.objectMapper = objectMapper;
    }

    public void handle(OrderCreatedEvent event) {
        if (processedEventRepository.existsById(event.eventId())) {
            log.info("Evento {} ya procesado, se ignora", event.eventId());
            return;
        }
        try {
            try {
                tx.executeWithoutResult(status -> {
                    processedEventRepository.saveAndFlush(new ProcessedEvent(event.eventId()));
                    for (OrderCreatedEvent.Item item : event.items()) {
                        if (productRepository.reserveStock(item.productId(), item.quantity()) == 0) {
                            // lanzar revierte los descuentos ya hechos a otros items del pedido
                            throw new InsufficientStockException(item.productId());
                        }
                        // se guarda lo reservado: es lo que se devuelve si el pago falla
                        stockReservationRepository.save(
                                new StockReservation(event.orderId(), item.productId(), item.quantity()));
                    }
                    outboxRepository.save(toOutbox(StockReservedEvent.TYPE, event.orderId(),
                            new StockReservedEvent(UUID.randomUUID(), event.orderId(), event.userId(),
                                    event.total(), Instant.now())));
                });
                log.info("Stock reservado para el pedido {}", event.orderId());
            } catch (InsufficientStockException e) {
                // la transaccion anterior se revirtio entera (incluida la inbox): se registra el rechazo
                tx.executeWithoutResult(status -> {
                    processedEventRepository.saveAndFlush(new ProcessedEvent(event.eventId()));
                    outboxRepository.save(toOutbox(StockRejectedEvent.TYPE, event.orderId(),
                            new StockRejectedEvent(UUID.randomUUID(), event.orderId(), Instant.now(),
                                    e.getMessage())));
                });
                log.info("Pedido {} rechazado: {}", event.orderId(), e.getMessage());
            }
        } catch (DataIntegrityViolationException e) {
            // otro hilo registro el mismo eventId a la vez: es un duplicado
            log.info("Evento {} duplicado detectado por la inbox, se ignora", event.eventId());
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

    static class InsufficientStockException extends RuntimeException {
        InsufficientStockException(Long productId) {
            super("Stock insuficiente o producto inexistente: " + productId);
        }
    }
}
