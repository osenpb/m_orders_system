package com.osen.msvc_product.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Publicado en stock-events (clave = orderId) cuando se reservo todo el stock del pedido.
 * userId y amount se copian del pedido (event-carried state transfer): asi payment
 * puede cobrar sin llamar a order.
 */
public record StockReservedEvent(UUID eventId, Long orderId, String userId, Double amount, Instant occurredAt) {

    public static final String TYPE = "StockReserved";
}
