package com.osen.msvc_product.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.osen.msvc_product.entity.StockReservation;
import com.osen.msvc_product.events.PaymentFailedEvent;
import com.osen.msvc_product.inbox.ProcessedEvent;
import com.osen.msvc_product.inbox.ProcessedEventRepository;
import com.osen.msvc_product.repository.ProductRepository;
import com.osen.msvc_product.repository.StockReservationRepository;

/**
 * Compensacion de la SAGA en product: si el pago del pedido fallo, devuelve al
 * inventario el stock que se habia reservado.
 *
 * Idempotente: la inbox (eventId) y el borrado de las reservas ocurren en la
 * misma transaccion que la devolucion, asi un reenvio no suma el stock dos veces.
 */
@Service
public class StockCompensationService {

    private static final Logger log = LoggerFactory.getLogger(StockCompensationService.class);

    private final ProductRepository productRepository;
    private final StockReservationRepository stockReservationRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final TransactionTemplate tx;

    public StockCompensationService(ProductRepository productRepository,
            StockReservationRepository stockReservationRepository,
            ProcessedEventRepository processedEventRepository, TransactionTemplate tx) {
        this.productRepository = productRepository;
        this.stockReservationRepository = stockReservationRepository;
        this.processedEventRepository = processedEventRepository;
        this.tx = tx;
    }

    public void releaseStock(PaymentFailedEvent event) {
        if (processedEventRepository.existsById(event.eventId())) {
            log.info("Evento {} ya procesado, se ignora", event.eventId());
            return;
        }
        try {
            tx.executeWithoutResult(status -> {
                processedEventRepository.saveAndFlush(new ProcessedEvent(event.eventId()));
                List<StockReservation> reservations = stockReservationRepository.findByOrderId(event.orderId());
                for (StockReservation r : reservations) {
                    productRepository.releaseStock(r.getProductId(), r.getQuantity());
                }
                stockReservationRepository.deleteByOrderId(event.orderId());
                log.info("Pago fallido del pedido {} ({}): {} reservas devueltas al inventario", event.orderId(),
                        event.reason(), reservations.size());
            });
        } catch (DataIntegrityViolationException e) {
            log.info("Evento {} duplicado detectado por la inbox, se ignora", event.eventId());
        }
    }
}
