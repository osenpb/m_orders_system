# Orders System Microservices

A distributed microservices architecture for managing products and orders, built with Spring Boot, Spring Cloud, and following best practices for cloud-native applications.

**Note:** `docker-compose.yml` provisions the whole stack: Keycloak (with its own Postgres), Zipkin, PostgreSQL (`orders_db` and `products_db`), Kafka (single-node KRaft) and all the services. The goal of the project is to practice the **Outbox** and **SAGA** patterns (see [Patrones de diseño en el flujo del proyecto](#patrones-de-diseño-en-el-flujo-del-proyecto)).

## Architecture

This system implements a microservices architecture with the following components:

- **API Gateway** - Single entry point for all client requests, routing and load balancing
- **Eureka Server** - Service discovery and registration
- **Config Server** - Centralized configuration management
- **Product Service** - Manages product catalog and inventory
- **Order Service** - Handles order processing and management

## Services Overview

### Services & Ports

| Service | Port | Description |
|---------|------|-------------|
| Eureka Server | 8761 | Service Discovery Server |
| Config Server | 8888 | Configuration Management Server |
| API Gateway | 8080 | API Gateway with OAuth2 |
| Product Service | 8081 | Product Management Service |
| Order Service | 8082 | Order Management Service |

### Microservices

#### Product Service (`msvc-product`)
Manages product catalog with CRUD operations for products including:
- Product listing and retrieval
- Product creation and updates
- Stock management
- Product pricing

#### Order Service (`msvc-order`)
Handles order processing with features including:
- Order creation and management
- Order item management
- Automatic total calculation
- Integration with Product Service via OpenFeign
- Circuit breaker pattern with Resilience4j

#### API Gateway
Central routing and security layer providing:
- Request routing to microservices
- OAuth2 token relay for authentication
- Load balancing
- Service discovery integration

#### Eureka Server
Service registry for microservices:
- Service registration
- Service discovery
- Health monitoring

#### Config Server
Centralized configuration management:
- Externalized configuration from Git repository
- Environment-specific configurations
- Dynamic configuration updates

## Tech Stack

- **Java 21**
- **Spring Boot 3.5.10**
- **Spring Cloud 2025.0.1**
- **Spring Cloud Gateway**
- **Spring Cloud Netflix Eureka**
- **Spring Cloud Config**
- **Spring Cloud OpenFeign**
- **Spring Data JPA**
- **PostgreSQL**
- **Apache Kafka** - Event messaging (Outbox + SAGA)
- **Resilience4j** - Circuit breaker pattern
- **Keycloak** - OAuth2/OIDC authentication
- **Micrometer Tracing** - Distributed tracing with OpenTelemetry
- **Zipkin** - Tracing visualization
- **Lombok**
- **Maven**
- **Docker**

## Prerequisites

Before running this application, ensure you have:

- Java 21 installed
- Maven 3.9+
- PostgreSQL 16 running on localhost:5432 (databases `orders_db` and `products_db`)
- Kafka running on localhost:29092 (both are provided by `docker compose up postgres kafka`)
- Keycloak server running on localhost:7080 (realm: order-system)
- Zipkin server running on localhost:9411 (optional, for tracing)
- Git (for config server)

## Setup

### Database Setup

Each service owns its own database. With Docker, `docker compose up postgres` creates `orders_db` and, through `postgres-init/`, `products_db` (the init script only runs when the volume is empty). Without Docker:

```sql
CREATE DATABASE orders_db;
CREATE DATABASE products_db;
```

Default credentials (override with `DB_USER` / `DB_PASSWORD`):
- Username: `postgres`
- Password: `postgres`

### Keycloak Setup

Ensure Keycloak is running with the following configuration:
- Realm: `order-system`
- Client: `api-gateway-client`
- Client Secret: `zUcCDu9kNswZAUMaCuaPIS3J2PPkaQOE`
- Scopes: `openid`, `profile`, `email`, `roles`

### Build the Project

Navigate to each microservice and build:

```bash
# Build all services
mvn clean install

# Or build individual services
cd msvc-product && mvn clean install
cd ../msvc-order && mvn clean install
cd ../api-gateway && mvn clean install
cd ../eureka-server && mvn clean install
cd ../config-server && mvn clean install
```

## Running the Application

### Start Services in Order

1. **Start Eureka Server** (Service Discovery)
   ```bash
   cd eureka-server
   mvn spring-boot:run
   ```
   Access: http://localhost:8761

2. **Start Config Server** (Configuration Management)
   ```bash
   cd config-server
   mvn spring-boot:run
   ```
   Access: http://localhost:8888

3. **Start Product Service**
   ```bash
   cd msvc-product
   mvn spring-boot:run
   ```
   Access: http://localhost:8081

4. **Start Order Service**
   ```bash
   cd msvc-order
   mvn spring-boot:run
   ```
   Access: http://localhost:8082

5. **Start API Gateway** (Last)
   ```bash
   cd api-gateway
   mvn spring-boot:run
   ```
   Access: http://localhost:8080

### Verify Services

Check Eureka Dashboard to ensure all services are registered:
- Open http://localhost:8761
- All services (config-server, msvc-product, msvc-order, api-gateway) should be listed

## API Endpoints

### Via API Gateway (Recommended)

All requests should go through the API Gateway for authentication and routing:

#### Products
- `GET http://localhost:8080/api/products` - List all products
- `GET http://localhost:8080/api/products/{id}` - Get product by ID
- `POST http://localhost:8080/api/products` - Create new product
- `PUT http://localhost:8080/api/products/{id}` - Update product
- `DELETE http://localhost:8080/api/products/{id}` - Delete product

#### Orders
- `GET http://localhost:8080/api/orders` - List all orders
- `GET http://localhost:8080/api/orders/{id}` - Get order by ID
- `POST http://localhost:8080/api/orders` - Create new order
- `PUT http://localhost:8080/api/orders/{id}` - Update order
- `DELETE http://localhost:8080/api/orders/{id}` - Delete order

### Direct Access (Without Authentication)

#### Product Service (Port 8081)
- `GET http://localhost:8081/api/products` - List all products
- `GET http://localhost:8081/api/products/{id}` - Get product by ID
- `POST http://localhost:8081/api/products` - Create new product
- `PUT http://localhost:8081/api/products/{id}` - Update product
- `DELETE http://localhost:8081/api/products/{id}` - Delete product

#### Order Service (Port 8082)
- `GET http://localhost:8082/api/orders` - List all orders
- `GET http://localhost:8082/api/orders/{id}` - Get order by ID
- `POST http://localhost:8082/api/orders` - Create new order
- `PUT http://localhost:8082/api/orders/{id}` - Update order
- `DELETE http://localhost:8082/api/orders/{id}` - Delete order

## Configuration

### Centralized Configuration

All microservices fetch configuration from the Config Server:
- Product Service: `msvc-product.yml`
- Order Service: `msvc-order.yml`
- API Gateway: `api-gateway.yml`
- Common configuration: `application.yml`

Configuration files are stored in a Git repository:
```
https://github.com/osenpb/config-server-order-system.git
```

### Environment Variables

You can override configuration using environment variables:
- `SERVER_PORT` - Server port
- `SPRING_APPLICATION_NAME` - Application name
- `EUREKA_INSTANCE_HOSTNAME` - Eureka instance hostname
- `SPRING_DATASOURCE_URL` - Database URL
- `SPRING_DATASOURCE_USERNAME` - Database username
- `SPRING_DATASOURCE_PASSWORD` - Database password

## Docker Deployment

Each microservice includes a Dockerfile for containerization:

### Build Docker Images

```bash
# Build all services
for service in eureka-server config-server msvc-product msvc-order api-gateway; do
  cd $service
  docker build -t orders-system/$service:latest .
  cd ..
done
```

### Run with Docker

```bash
# Run each service
docker run -p 8761:8761 orders-system/eureka-server:latest
docker run -p 8888:8888 orders-system/config-server:latest
docker run -p 8081:8081 orders-system/msvc-product:latest
docker run -p 8082:8082 orders-system/msvc-order:latest
docker run -p 8080:8080 orders-system/api-gateway:latest
```

## Monitoring & Tracing

### Distributed Tracing

The system uses Micrometer Tracing with OpenTelemetry and Zipkin:

- Zipkin Server: http://localhost:9411
- Tracing probability: 100% (configurable in `application.yml`)

### Actuator Endpoints

Each service exposes Spring Boot Actuator endpoints:
- `/actuator/health` - Service health status
- `/actuator/info` - Service information
- `/actuator/tracing` - Tracing information

## Security

### Authentication & Authorization

The system uses OAuth2/OIDC with Keycloak:
- Authentication is handled at the API Gateway level
- Token Relay pattern propagates tokens to microservices
- Microservices act as OAuth2 Resource Servers

### Security Configuration

- API Gateway uses `spring-boot-starter-oauth2-client` for authentication
- Microservices use `spring-boot-starter-oauth2-resource-server` for token validation
- JWT tokens validated against Keycloak issuer URI

## Resilience Patterns

### Circuit Breaker

The Order Service implements the Circuit Breaker pattern using Resilience4j:
- Configured for `productCB` instance (Product Service calls)
- Sliding window size: 10 calls
- Failure rate threshold: 50%
- Wait duration in open state: 5 seconds
- Permitted calls in half-open state: 3

## Patrones de diseño en el flujo del proyecto

El proyecto sirve para practicar **outbox y SAGA**. Este es el recorrido de un pedido y el patrón que actúa en cada paso (resumen; los detalles están en el código).

```
Cliente ──▶ API Gateway ──▶ msvc-order ──Feign──▶ msvc-product      (validar producto y precio)
                               │
                               │ 1 transacción: Order(PENDING) + outbox_event(OrderCreated)
                               ▼
                         OutboxPublisher ──▶ Kafka [order-events] ──▶ msvc-product
                                                                        │ 1 transacción: inbox + reserva de stock + outbox_event
                                                                        ▼
msvc-order ◀── Kafka [stock-events] ◀── OutboxPublisher ◀───── StockReserved | StockRejected
   │
   └─▶ Order: CONFIRMED  (StockReserved)   o   REJECTED  (StockRejected = compensación)
```

### Infraestructura (antes de que llegue el pedido)

| Patrón | Dónde actúa | Para qué |
|--------|-------------|----------|
| **API Gateway + Token Relay** | `api-gateway` | Punto de entrada único: enruta y propaga el JWT de Keycloak a los servicios. |
| **Service Discovery** | `eureka-server`, `@FeignClient(name = "msvc-product")` | `order` encuentra a `product` por nombre, sin IPs fijas. |
| **Externalized Configuration** | `config-server` + repo `config` | La configuración (BD, Kafka, circuit breaker) vive fuera de los servicios, por perfil (`docker`). |
| **Database per Service** | `orders_db` y `products_db` | Cada servicio es dueño de sus datos; solo se comunican por API o eventos. Sin esto la SAGA no tiene sentido. |

### Paso 1 · Crear el pedido (`msvc-order`)

- **Circuit Breaker** (`ProductGateway`, instancia `productCB`): protege la consulta síncrona a `product`. No hay *fallback* con datos inventados: si el producto no existe responde 422 (y no cuenta como fallo del breaker) y si `product` no responde, 503. El breaker vive en su propio bean para que pase por el proxy de Spring.
- **Server-side pricing**: el precio se toma de `product`, nunca de la petición del cliente.
- **Transactional Outbox** (`OutboxEvent`, `createNewOrder`): el pedido y el evento `OrderCreated` se guardan **en la misma transacción**. Resuelve el problema de "guardé en la BD pero no pude publicar en Kafka" (escritura doble). Nunca se publica directo a Kafka desde la lógica de negocio.
- **Polling Publisher** (`OutboxPublisher`): un `@Scheduled` lee los eventos sin publicar, los envía y marca `publishedAt`. La clave del mensaje es el `orderId`, así que los eventos de un mismo pedido llegan en orden (misma partición). Si Kafka cae, el lote se corta y se reintenta sin desordenar.
- **Estado como máquina simple**: el pedido nace `PENDING` y solo transiciona a `CONFIRMED` o `REJECTED` desde `PENDING`.

### Paso 2 · Reservar stock (`msvc-product`)

- **Idempotent Consumer / Inbox** (`processed_events`): Kafka entrega **al menos una vez**, así que el mismo evento puede llegar dos veces. El `eventId` se inserta en la inbox **en la misma transacción** que el descuento de stock; la clave primaria impide el duplicado.
- **Atomic conditional update** (`ProductRepository.reserveStock`): `UPDATE ... SET stock = stock - :q WHERE stock >= :q`. Evita la carrera de leer-comprobar-escribir cuando dos reservas llegan a la vez.
- **Todo o nada**: si un ítem del pedido no tiene stock, se revierte la transacción completa (incluidos los descuentos de los otros ítems) y se registra el rechazo.
- **Transactional Outbox** también aquí: `StockReserved` / `StockRejected` se escriben en el outbox de `product` y los publica su propio `OutboxPublisher` en `stock-events`.

### Paso 3 · Cerrar la SAGA (`msvc-order`)

- **SAGA coreografiada** (`OrderSagaService`, `StockEventsListener`): no hay un orquestador central; cada servicio reacciona a eventos del otro. `StockReserved` confirma el pedido.
- **Compensación**: `StockRejected` deshace lo que `order` ya había hecho (el pedido pendiente pasa a `REJECTED`). La compensación es un evento de negocio, no un rollback de BD.
- **Inbox** también en `order`, para que el cambio de estado sea idempotente.

### Garantías y límites actuales

- Entrega **al menos una vez** + consumidores idempotentes = efecto **exactamente una vez** sobre el stock y el estado del pedido.
- `OutboxPublisher` está pensado para **una sola instancia** de cada servicio (con varias réplicas habría que bloquear el lote, p. ej. `FOR UPDATE SKIP LOCKED`).
- Un mensaje que no se pueda deserializar se reintenta y luego se descarta: aún no hay DLQ.

### Pendiente (según el plan)

- `msvc-payment` (`StockReserved → PaymentCompleted | PaymentFailed`) y compensaciones encadenadas (liberar stock si el pago falla).
- **Dead Letter Queue** y reintentos con *backoff*.
- Sustituir el polling por **CDC con Debezium** y comparar con **orquestación**.

## Project Structure

```
orders_system/
├── api-gateway/          # API Gateway service
├── config/               # Configuration files for config-server
├── config-server/        # Spring Cloud Config Server
├── eureka-server/        # Service Discovery Server
├── msvc-order/          # Order Management Microservice
├── msvc-product/        # Product Management Microservice
├── postgres-init/       # SQL run on first Postgres start (creates products_db)
├── .gitignore
├── .dockerignore
└── README.md
```

## Data Models

### Product
- `id` - Long (auto-generated)
- `name` - String
- `stock` - Integer
- `price` - Double

### Order
- `id` - Long (auto-generated)
- `userId` - String
- `status` - `PENDING` (default), `CONFIRMED`, `REJECTED`, `CANCELLED`
- `total` - Double (auto-calculated from prices taken from Product Service)
- `items` - List<OrderItem>

### OrderItem
- `id` - Long (auto-generated)
- `productId` - Long
- `name` - String
- `price` - Double
- `quantity` - Integer
- `order` - Order (relationship)

## Troubleshooting

### Common Issues

**Services not registering with Eureka:**
- Ensure Eureka Server is running first
- Check application names in configuration
- Verify `eureka.client.service-url.defaultZone` is correct

**Config Server not loading configurations:**
- Verify Git repository is accessible
- Check branch and file names in Git
- Review Config Server logs for errors

**Database connection errors:**
- Ensure PostgreSQL is running and `orders_db` / `products_db` exist
- Verify database credentials
- Check database URL in configuration

**API Gateway routing issues:**
- Verify service names in route configuration
- Check that microservices are registered in Eureka
- Review Gateway logs for routing errors

## Development

### Code Style
- Project uses Lombok to reduce boilerplate code
- Follow Spring Boot conventions
- RESTful API design principles

### Adding New Microservices
1. Create new Spring Boot project with required dependencies
2. Add Eureka Client dependency
3. Add Config Client dependency
4. Configure service in Config Server
5. Add route in API Gateway
6. Start service after Eureka and Config Server


