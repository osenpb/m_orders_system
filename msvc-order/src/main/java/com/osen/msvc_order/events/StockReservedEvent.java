package com.osen.msvc_order.events;

import java.time.Instant;
import java.util.UUID;

/** Copia del contrato publicado por msvc-product en stock-events. */
public record StockReservedEvent(UUID eventId, Long orderId, Instant occurredAt) {

    public static final String TYPE = "StockReserved";
}
