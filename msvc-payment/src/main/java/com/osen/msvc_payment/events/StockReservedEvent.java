package com.osen.msvc_payment.events;

import java.time.Instant;
import java.util.UUID;

/** Copia del contrato publicado por msvc-product en stock-events. */
public record StockReservedEvent(UUID eventId, Long orderId, String userId, Double amount, Instant occurredAt) {

    public static final String TYPE = "StockReserved";
}
