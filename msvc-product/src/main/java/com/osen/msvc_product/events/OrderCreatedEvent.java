package com.osen.msvc_product.events;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Copia del contrato publicado por msvc-order en el topic order-events. */
public record OrderCreatedEvent(
        UUID eventId,
        Long orderId,
        String userId,
        Instant occurredAt,
        List<Item> items) {

    public static final String TYPE = "OrderCreated";

    public record Item(Long productId, Integer quantity) {
    }
}
