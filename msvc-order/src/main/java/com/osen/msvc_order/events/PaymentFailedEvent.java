package com.osen.msvc_order.events;

import java.time.Instant;
import java.util.UUID;

/** Copia del contrato publicado por msvc-payment en payment-events. */
public record PaymentFailedEvent(UUID eventId, Long orderId, String reason, Instant occurredAt) {

    public static final String TYPE = "PaymentFailed";
}
