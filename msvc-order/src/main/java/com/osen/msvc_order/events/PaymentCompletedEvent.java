package com.osen.msvc_order.events;

import java.time.Instant;
import java.util.UUID;

/** Copia del contrato publicado por msvc-payment en payment-events. */
public record PaymentCompletedEvent(UUID eventId, Long orderId, Double amount, Instant occurredAt) {

    public static final String TYPE = "PaymentCompleted";
}
