package com.osen.msvc_product.events;

import java.time.Instant;
import java.util.UUID;

/** Publicado en stock-events (clave = orderId) cuando se reservo todo el stock del pedido. */
public record StockReservedEvent(UUID eventId, Long orderId, Instant occurredAt) {

    public static final String TYPE = "StockReserved";
}
