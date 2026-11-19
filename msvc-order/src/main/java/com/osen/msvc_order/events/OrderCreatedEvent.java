package com.osen.msvc_order.events;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Contrato del evento OrderCreated (topic order-events, clave = orderId).
 * eventId identifica el evento de forma unica: los consumidores lo usan para
 * ser idempotentes.
 */
public record OrderCreatedEvent(
        UUID eventId,
        Long orderId,
        String userId,
        Double total,
        Instant occurredAt,
        List<Item> items) {

    public static final String TYPE = "OrderCreated";

    public record Item(Long productId, Integer quantity) {
    }
}
