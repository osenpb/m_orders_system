package com.osen.msvc_payment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.osen.msvc_payment.entity.PaymentStatus;
import com.osen.msvc_payment.events.StockReservedEvent;
import com.osen.msvc_payment.outbox.OutboxRepository;
import com.osen.msvc_payment.repository.PaymentRepository;
import com.osen.msvc_payment.service.PaymentService;

/** Regla de cobro, outbox e idempotencia contra un Postgres descartable. Sin Kafka. */
@Testcontainers
@SpringBootTest(properties = { "spring.cloud.config.enabled=false", "spring.config.import=",
        "eureka.client.enabled=false", "spring.cloud.discovery.enabled=false",
        "spring.jpa.hibernate.ddl-auto=update", "spring.kafka.listener.auto-startup=false",
        "outbox.poll-interval-ms=3600000", "management.tracing.enabled=false" })
class PaymentServiceIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    PaymentService service;
    @Autowired
    PaymentRepository payments;
    @Autowired
    OutboxRepository outbox;

    private StockReservedEvent event(UUID id, long orderId, double amount) {
        return new StockReservedEvent(id, orderId, "user-1", amount, Instant.now());
    }

    private long outboxCount(long orderId, String type) {
        return outbox.findAll().stream()
                .filter(o -> o.getAggregateId().equals(String.valueOf(orderId)) && o.getType().equals(type)).count();
    }

    private long paymentsOf(long orderId) {
        return payments.findAll().stream().filter(x -> x.getOrderId() == orderId).count();
    }

    @Test
    void normalAmountIsCompleted() {
        service.handle(event(UUID.randomUUID(), 1, 500));
        var p = payments.findAll().stream().filter(x -> x.getOrderId() == 1).findFirst().orElseThrow();
        assertThat(p.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(outboxCount(1, "PaymentCompleted")).isEqualTo(1);
        assertThat(outboxCount(1, "PaymentFailed")).isZero();
    }

    @Test
    void amountOverLimitFails() {
        service.handle(event(UUID.randomUUID(), 2, 1500));
        var p = payments.findAll().stream().filter(x -> x.getOrderId() == 2).findFirst().orElseThrow();
        assertThat(p.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(outboxCount(2, "PaymentFailed")).isEqualTo(1);
        assertThat(outboxCount(2, "PaymentCompleted")).isZero();
    }

    @Test
    void limitIsInclusive() {
        service.handle(event(UUID.randomUUID(), 3, 1000));
        assertThat(outboxCount(3, "PaymentCompleted")).isEqualTo(1);
    }

    @Test
    void sameEventTwiceChargesOnce() {
        UUID id = UUID.randomUUID();
        service.handle(event(id, 4, 100));
        service.handle(event(id, 4, 100));
        assertThat(paymentsOf(4)).isEqualTo(1);
        assertThat(outboxCount(4, "PaymentCompleted")).isEqualTo(1);
    }

    @Test
    void sameOrderWithDifferentEventIdIsNotChargedTwice() {
        service.handle(event(UUID.randomUUID(), 5, 100));
        service.handle(event(UUID.randomUUID(), 5, 100));
        assertThat(paymentsOf(5)).isEqualTo(1);
        assertThat(outboxCount(5, "PaymentCompleted")).isEqualTo(1);
    }
}
