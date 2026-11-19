package com.osen.msvc_product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.osen.msvc_product.entity.Product;
import com.osen.msvc_product.events.OrderCreatedEvent;
import com.osen.msvc_product.events.PaymentFailedEvent;
import com.osen.msvc_product.outbox.OutboxEvent;
import com.osen.msvc_product.outbox.OutboxRepository;
import com.osen.msvc_product.repository.ProductRepository;
import com.osen.msvc_product.repository.StockReservationRepository;

/**
 * Prueba la mensajeria real de product contra un Postgres y un Kafka
 * descartables (Testcontainers). No necesita config-server, eureka ni el resto
 * de servicios.
 */
@Testcontainers
@SpringBootTest(properties = { "spring.cloud.config.enabled=false", "spring.cloud.config.import-check.enabled=false",
        "spring.config.import=", "eureka.client.enabled=false", "spring.cloud.discovery.enabled=false",
        "spring.jpa.hibernate.ddl-auto=update", "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.producer.acks=all", "management.tracing.enabled=false",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:1/jwks" })
class SagaMessagingIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    // puerto fijo del host para poder anunciarlo en advertised.listeners (misma config que el compose)
    static final int KAFKA_PORT = freePort();

    @Container
    static GenericContainer<?> kafka = new GenericContainer<>("apache/kafka:3.9.0")
            .withEnv("KAFKA_NODE_ID", "1")
            .withEnv("KAFKA_PROCESS_ROLES", "broker,controller")
            .withEnv("KAFKA_CONTROLLER_QUORUM_VOTERS", "1@localhost:9093")
            .withEnv("KAFKA_LISTENERS", "PLAINTEXT://:9092,CONTROLLER://:9093")
            .withEnv("KAFKA_ADVERTISED_LISTENERS", "PLAINTEXT://localhost:" + KAFKA_PORT)
            .withEnv("KAFKA_LISTENER_SECURITY_PROTOCOL_MAP", "CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT")
            .withEnv("KAFKA_CONTROLLER_LISTENER_NAMES", "CONTROLLER")
            .withEnv("KAFKA_INTER_BROKER_LISTENER_NAME", "PLAINTEXT")
            .withEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", "1")
            .withEnv("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", "1")
            .withEnv("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", "1")
            .withEnv("KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS", "0")
            .withExposedPorts(9092)
            .withCreateContainerCmdModifier(cmd -> cmd.getHostConfig()
                    .withPortBindings(new PortBinding(Ports.Binding.bindPort(KAFKA_PORT), new ExposedPort(9092))))
            .waitingFor(Wait.forLogMessage(".*Kafka Server started.*", 1));

    static int freePort() {
        try (var s = new java.net.ServerSocket(0)) {
            return s.getLocalPort();
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    static String bootstrap() {
        return "localhost:" + KAFKA_PORT;
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
        r.add("spring.kafka.bootstrap-servers", SagaMessagingIT::bootstrap);
    }

    @Autowired
    KafkaTemplate<String, String> template;
    @Autowired
    ProductRepository products;
    @Autowired
    OutboxRepository outbox;
    @Autowired
    StockReservationRepository reservations;
    @Autowired
    ObjectMapper mapper;

    private Product newProduct(int stock) {
        return products.save(new Product(null, "p-" + UUID.randomUUID(), stock, 10.0));
    }

    private int stockOf(Product p) {
        return products.findById(p.getId()).orElseThrow().getStock();
    }

    private void sendOrder(UUID eventId, long orderId, double total, OrderCreatedEvent.Item... items)
            throws Exception {
        var event = new OrderCreatedEvent(eventId, orderId, "user-1", total, Instant.now(), List.of(items));
        var record = new ProducerRecord<>("order-events", String.valueOf(orderId), mapper.writeValueAsString(event));
        record.headers().add("eventType", "OrderCreated".getBytes(StandardCharsets.UTF_8));
        template.send(record).get();
    }

    private void sendPaymentFailed(UUID eventId, long orderId) throws Exception {
        var event = new PaymentFailedEvent(eventId, orderId, "limit", Instant.now());
        var record = new ProducerRecord<>("payment-events", String.valueOf(orderId),
                mapper.writeValueAsString(event));
        record.headers().add("eventType", "PaymentFailed".getBytes(StandardCharsets.UTF_8));
        template.send(record).get();
    }

    private List<OutboxEvent> outboxOf(long orderId, String type) {
        return outbox.findAll().stream()
                .filter(o -> o.getAggregateId().equals(String.valueOf(orderId)) && o.getType().equals(type))
                .toList();
    }

    @Test
    void duplicateOrderCreatedReservesStockOnlyOnce() throws Exception {
        Product p = newProduct(10);
        UUID eventId = UUID.randomUUID();
        // misma clave => misma particion => se procesan en orden; el 3er evento actua de centinela
        sendOrder(eventId, 1001, 20, new OrderCreatedEvent.Item(p.getId(), 2));
        sendOrder(eventId, 1001, 20, new OrderCreatedEvent.Item(p.getId(), 2));
        sendOrder(UUID.randomUUID(), 1001, 10, new OrderCreatedEvent.Item(p.getId(), 1));

        await().atMost(Duration.ofSeconds(30)).until(() -> stockOf(p) == 7);
        assertThat(stockOf(p)).isEqualTo(7); // 10 - 2 (una sola vez) - 1
        assertThat(outboxOf(1001, "StockReserved")).hasSize(2);
    }

    @Test
    void insufficientStockRejectsAndKeepsStockIntact() throws Exception {
        Product ok = newProduct(5);
        Product scarce = newProduct(1);
        sendOrder(UUID.randomUUID(), 1002, 50, new OrderCreatedEvent.Item(ok.getId(), 3),
                new OrderCreatedEvent.Item(scarce.getId(), 5));

        await().atMost(Duration.ofSeconds(30)).until(() -> !outboxOf(1002, "StockRejected").isEmpty());
        // todo o nada: el descuento del primer item se revirtio
        assertThat(stockOf(ok)).isEqualTo(5);
        assertThat(stockOf(scarce)).isEqualTo(1);
        assertThat(outboxOf(1002, "StockReserved")).isEmpty();
        assertThat(reservations.findByOrderId(1002L)).isEmpty();
    }

    @Test
    void paymentFailedReleasesReservedStockOnlyOnce() throws Exception {
        Product p = newProduct(10);
        sendOrder(UUID.randomUUID(), 1003, 40, new OrderCreatedEvent.Item(p.getId(), 4));
        await().atMost(Duration.ofSeconds(30)).until(() -> stockOf(p) == 6);

        UUID failId = UUID.randomUUID();
        sendPaymentFailed(failId, 1003);
        sendPaymentFailed(failId, 1003); // duplicado
        await().atMost(Duration.ofSeconds(30)).until(() -> stockOf(p) == 10);
        // centinela: un fallo de otro pedido, tras el duplicado, para saber que ya se consumio
        sendPaymentFailed(UUID.randomUUID(), 1999);
        Thread.sleep(3000);
        assertThat(stockOf(p)).isEqualTo(10); // no 14
        assertThat(reservations.findByOrderId(1003L)).isEmpty();
    }

    @Test
    void corruptMessageGoesStraightToDlt() throws Exception {
        template.send("order-events", "9999", "{esto no es json").get();

        Properties cfg = new Properties();
        cfg.put("bootstrap.servers", bootstrap());
        cfg.put("group.id", "dlt-test-" + UUID.randomUUID());
        cfg.put("auto.offset.reset", "earliest");
        cfg.put("key.deserializer", StringDeserializer.class.getName());
        cfg.put("value.deserializer", StringDeserializer.class.getName());
        try (var consumer = new KafkaConsumer<String, String>(cfg)) {
            consumer.subscribe(List.of("order-events.DLT"));
            ConsumerRecord<String, String> found = null;
            long deadline = System.currentTimeMillis() + 30_000;
            while (found == null && System.currentTimeMillis() < deadline) {
                for (var r : consumer.poll(Duration.ofSeconds(1))) {
                    if ("{esto no es json".equals(r.value())) {
                        found = r;
                    }
                }
            }
            assertThat(found).as("mensaje corrupto en order-events.DLT").isNotNull();
            assertThat(found.headers().lastHeader("kafka_dlt-original-topic")).isNotNull();
        }
    }

    @Test
    void outboxPublisherSendsPendingEventsAndMarksThem() throws Exception {
        UUID id = UUID.randomUUID();
        outbox.save(new OutboxEvent(id, "7777", "StockReserved", "{\"probe\":true}"));

        await().atMost(Duration.ofSeconds(30))
                .until(() -> outbox.findById(id).orElseThrow().getPublishedAt() != null);

        Properties cfg = new Properties();
        cfg.put("bootstrap.servers", bootstrap());
        cfg.put("group.id", "outbox-test-" + UUID.randomUUID());
        cfg.put("auto.offset.reset", "earliest");
        cfg.put("key.deserializer", StringDeserializer.class.getName());
        cfg.put("value.deserializer", StringDeserializer.class.getName());
        try (var consumer = new KafkaConsumer<String, String>(cfg)) {
            consumer.subscribe(List.of("stock-events"));
            boolean seen = false;
            long deadline = System.currentTimeMillis() + 20_000;
            while (!seen && System.currentTimeMillis() < deadline) {
                for (var r : consumer.poll(Duration.ofSeconds(1))) {
                    seen |= "7777".equals(r.key()) && "{\"probe\":true}".equals(r.value())
                            && new String(r.headers().lastHeader("eventType").value(), StandardCharsets.UTF_8)
                                    .equals("StockReserved");
                }
            }
            assertThat(seen).isTrue();
        }
    }
}
