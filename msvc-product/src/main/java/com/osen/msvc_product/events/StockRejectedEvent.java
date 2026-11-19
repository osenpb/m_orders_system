package com.osen.msvc_product.events;

import java.time.Instant;
import java.util.UUID;

/** Publicado en stock-events (clave = orderId) cuando no se pudo reservar el pedido completo. */
public record StockRejectedEvent(UUID eventId, Long orderId, Instant occurredAt, String reason) {

    public static final String TYPE = "StockRejected";
}
