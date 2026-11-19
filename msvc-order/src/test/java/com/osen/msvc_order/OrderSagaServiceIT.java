package com.osen.msvc_order;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.osen.msvc_order.entities.Order;
import com.osen.msvc_order.entities.OrderStatus;
import com.osen.msvc_order.repositories.OrderRepository;
import com.osen.msvc_order.services.OrderSagaService;

/** Cierre de la SAGA en order contra un Postgres descartable. Sin Kafka. */
@Testcontainers
@SpringBootTest(properties = { "spring.cloud.config.enabled=false", "spring.config.import=",
        "eureka.client.enabled=false", "spring.cloud.discovery.enabled=false",
        "spring.jpa.hibernate.ddl-auto=update", "spring.kafka.listener.auto-startup=false",
        "outbox.poll-interval-ms=3600000", "management.tracing.enabled=false",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:1/jwks" })
class OrderSagaServiceIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    OrderSagaService saga;
    @Autowired
    OrderRepository orders;

    private Order pending() {
        return orders.save(new Order(null, "user-1", 100, List.of()));
    }

    private OrderStatus statusOf(Order o) {
        return orders.findById(o.getId()).orElseThrow().getStatus();
    }

    @Test
    void paymentCompletedConfirmsOrder() {
        Order o = pending();
        saga.onPaymentCompleted(UUID.randomUUID(), o.getId());
        assertThat(statusOf(o)).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void paymentFailedRejectsOrder() {
        Order o = pending();
        saga.onPaymentFailed(UUID.randomUUID(), o.getId(), "limit");
        assertThat(statusOf(o)).isEqualTo(OrderStatus.REJECTED);
    }

    @Test
    void stockRejectedRejectsOrder() {
        Order o = pending();
        saga.onStockRejected(UUID.randomUUID(), o.getId(), "no stock");
        assertThat(statusOf(o)).isEqualTo(OrderStatus.REJECTED);
    }

    @Test
    void finalStateIsNotOverwrittenByLateEvents() {
        Order o = pending();
        saga.onPaymentFailed(UUID.randomUUID(), o.getId(), "limit");
        saga.onPaymentCompleted(UUID.randomUUID(), o.getId());
        assertThat(statusOf(o)).isEqualTo(OrderStatus.REJECTED);
    }

    @Test
    void duplicateEventIsIgnored() {
        Order o = pending();
        UUID id = UUID.randomUUID();
        saga.onPaymentCompleted(id, o.getId());
        saga.onPaymentCompleted(id, o.getId());
        assertThat(statusOf(o)).isEqualTo(OrderStatus.CONFIRMED);
    }
}
