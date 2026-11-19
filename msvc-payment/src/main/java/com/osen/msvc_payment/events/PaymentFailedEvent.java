package com.osen.msvc_payment.events;

import java.time.Instant;
import java.util.UUID;

/** Publicado en payment-events (clave = orderId) cuando el cobro fallo: dispara las compensaciones. */
public record PaymentFailedEvent(UUID eventId, Long orderId, String reason, Instant occurredAt) {

    public static final String TYPE = "PaymentFailed";
}
