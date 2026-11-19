package com.osen.msvc_order.services;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.osen.msvc_order.entities.OrderStatus;
import com.osen.msvc_order.inbox.ProcessedEvent;
import com.osen.msvc_order.inbox.ProcessedEventRepository;
import com.osen.msvc_order.repositories.OrderRepository;

/**
 * Cierre de la SAGA en order: aplica el resultado de la reserva de stock.
 * StockReserved confirma el pedido; StockRejected lo rechaza (compensacion).
 * Idempotente por eventId (inbox) y solo transiciona desde PENDING.
 */
@Service
public class OrderSagaService {

    private static final Logger log = LoggerFactory.getLogger(OrderSagaService.class);

    private final OrderRepository orderRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final TransactionTemplate tx;

    public OrderSagaService(OrderRepository orderRepository, ProcessedEventRepository processedEventRepository,
            TransactionTemplate tx) {
        this.orderRepository = orderRepository;
        this.processedEventRepository = processedEventRepository;
        this.tx = tx;
    }

    public void onStockReserved(UUID eventId, Long orderId) {
        transition(eventId, orderId, OrderStatus.CONFIRMED);
    }

    public void onStockRejected(UUID eventId, Long orderId, String reason) {
        log.info("Pedido {} rechazado por product: {}", orderId, reason);
        transition(eventId, orderId, OrderStatus.REJECTED);
    }

    private void transition(UUID eventId, Long orderId, OrderStatus target) {
        if (processedEventRepository.existsById(eventId)) {
            log.info("Evento {} ya procesado, se ignora", eventId);
            return;
        }
        try {
            tx.executeWithoutResult(status -> {
                processedEventRepository.saveAndFlush(new ProcessedEvent(eventId));
                orderRepository.findById(orderId).ifPresentOrElse(order -> {
                    if (order.getStatus() == OrderStatus.PENDING) {
                        order.setStatus(target);
                        log.info("Pedido {} -> {}", orderId, target);
                    } else {
                        log.warn("Pedido {} en estado {}, se ignora la transicion a {}", orderId,
                                order.getStatus(), target);
                    }
                }, () -> log.warn("Pedido {} no existe, evento {} ignorado", orderId, eventId));
            });
        } catch (DataIntegrityViolationException e) {
            log.info("Evento {} duplicado detectado por la inbox, se ignora", eventId);
        }
    }
}
