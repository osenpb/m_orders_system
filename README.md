# Orders System Microservices

A Spring Boot / Spring Cloud microservices system for products and orders. Its main goal is to practice the **Transactional Outbox** and **SAGA** patterns on top of Kafka.

## Services

| Service | Port | Role |
|---------|------|------|
| `api-gateway` | 8080 | Single entry point, OAuth2 (Keycloak) |
| `eureka-server` | 8761 | Service discovery |
| `config-server` | 8888 | Centralized configuration (Git repo in `config/`) |
| `msvc-product` | 8081 | Product catalog and stock |
| `msvc-order` | 8082 | Orders, SAGA entry and exit point |
| `msvc-payment` | 8083 | Simulated payments, SAGA participant (no REST API) |

**Stack:** Java 21, Spring Boot 3.5, Spring Cloud 2025.0, PostgreSQL, Kafka (KRaft), Resilience4j, Keycloak, Micrometer Tracing + Zipkin, Maven, Docker.

## Patterns and the flow they cover

### Infrastructure patterns (before a request reaches business logic)

| Pattern | Where | What it does |
|---------|-------|--------------|
| **API Gateway + Token Relay** | `api-gateway` | Routes requests and forwards the Keycloak JWT to the services. |
| **Service Discovery** | `eureka-server`, `@FeignClient("msvc-product")` | Services find each other by name, no hardcoded hosts. |
| **Externalized Configuration** | `config-server` | DB, Kafka and breaker settings live outside the services, per profile. |
| **Database per Service** | `orders_db`, `products_db`, `payments_db` | Each service owns its data; communication is only via API or events. |

### Order flow (SAGA)

```
Client -> Gateway -> msvc-order --Feign--> msvc-product        (validate product and price)
                        |
                        | 1 TX: Order(PENDING) + outbox(OrderCreated)
                        v
                  Outbox publisher -> [order-events] -> msvc-product
                                                           | 1 TX: inbox + reserve stock + outbox
                        StockRejected -> order: REJECTED   | (compensation 1)
                        StockReserved -> [stock-events] -> msvc-payment
                                                           | 1 TX: inbox + payment + outbox
msvc-order <- [payment-events] <- PaymentCompleted | PaymentFailed
   |                                   '-> msvc-product (PaymentFailed): release stock (compensation 2)
   '-> CONFIRMED (PaymentCompleted) or REJECTED (PaymentFailed)
```

| Step | Pattern | Behavior |
|------|---------|----------|
| 1. Create order (`msvc-order`) | **Circuit Breaker** (Resilience4j) | Protects the synchronous call to product: 422 if the product does not exist, 503 if product is down. No fake fallback data. |
| | **Server-side pricing** | Price always comes from product, never from the client. |
| | **Transactional Outbox** | Order and `OrderCreated` event are saved in the same transaction, so there is no DB-vs-Kafka dual write. |
| | **Polling Publisher** | A scheduled job publishes unsent outbox events, keyed by `orderId` to keep per-order ordering. |
| 2. Reserve stock (`msvc-product`) | **Idempotent Consumer (Inbox)** | The event id is stored in the same transaction as the stock change, so redelivered events are ignored. |
| | **Atomic conditional update** | `UPDATE ... SET stock = stock - :q WHERE stock >= :q` avoids race conditions; all-or-nothing per order. |
| | **Event-carried state transfer** | `StockReserved` carries user and amount, so payment never calls order synchronously. |
| 3. Charge (`msvc-payment`) | **SAGA participant** | Simulated charge; amounts above `payment.max-amount` (default 1000) fail, to exercise the failure path. One payment per order. |
| 4. Close and compensate | **Choreographed SAGA** | No central orchestrator; each service reacts to events. Only `PaymentCompleted` confirms the order. |
| | **Compensating transactions** | `StockRejected` -> order `REJECTED`. `PaymentFailed` -> order `REJECTED` and product releases the reserved stock. |
| Any consumer | **Retry with backoff + Dead Letter Topic** | 4 retries (1s, 2s, 4s, 8s), then the message goes to `<topic>.DLT`; non-recoverable errors go straight to the DLT. |

Delivery is at-least-once, and idempotent consumers make the effect exactly-once on stock and order state.

## Running

Requirements: Java 21, Maven 3.9+, Docker.

```bash
docker compose up -d        # Keycloak, Zipkin, Postgres, Kafka and all services
```

For local development, start only the infrastructure (`docker compose up -d postgres kafka keycloak zipkin`), then run `mvn spring-boot:run` in this order: `eureka-server`, `config-server`, `msvc-product`, `msvc-order`, `msvc-payment`, `api-gateway`. Check registration at http://localhost:8761.

Configure Keycloak (realm `order-system`, client `api-gateway-client`) and keep secrets in `.env`, not in the repo.

### API (through the gateway, `http://localhost:8080`)

- `/api/products` - `GET`, `POST`; `/api/products/{id}` - `GET`, `PUT`, `DELETE`
- `/api/orders` - `GET`, `POST`; `/api/orders/{id}` - `GET`, `PUT`, `DELETE`

Order status: `PENDING` -> `CONFIRMED` | `REJECTED`.

### Tests

```bash
mvn test -Dtest='*IT'   # inside msvc-product, msvc-payment or msvc-order (Testcontainers)
```

## Current limits

- Each outbox publisher assumes a single instance (multiple replicas would need `FOR UPDATE SKIP LOCKED`).
- Published outbox events are never purged.
- Schemas use `ddl-auto: update`, not Flyway migrations.
- Planned: replace polling with CDC (Debezium) and compare with orchestration.
