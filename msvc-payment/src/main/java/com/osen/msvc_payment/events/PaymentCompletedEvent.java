package com.osen.msvc_payment.events;

import java.time.Instant;
import java.util.UUID;

/** Publicado en payment-events (clave = orderId) cuando el cobro salio bien. */
public record PaymentCompletedEvent(UUID eventId, Long orderId, Double amount, Instant occurredAt) {

    public static final String TYPE = "PaymentCompleted";
}
