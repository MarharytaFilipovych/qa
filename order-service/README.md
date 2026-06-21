# Practice 4: Cafeteria Delivery

**Cafeteria Delivery** represents a modular monolith for placing and managing cafeteria food orders.

- **Core Item:** `Order`
- **Core Action:** `CreateOrder`

---
## Domain Rules

1. **OwnerUserId id is required**
2. **Item name cannot be blank**
3. **Quantity must be at least 1**
4. **Price cannot be negative**
These rules are enforced within `Order` domain entity with the help of annotations.
5. **Status transitions are strictly limited** — `PENDING → CONFIRMED/CANCELLED`, `CONFIRMED → DELIVERED/CANCELLED`. Terminal states (`DELIVERED`, `CANCELLED`) cannot be changed. Enforced within `Order.changeStatus()` method.
---

## Architecture

src/main/java/com/cafeteria/:
* api/ ->  HTTP layer (no business logic): OrderController, HealthController
  * exception/ -> ErrorResponse, GlobalExceptionHandler
  * filter/ -> CorrelationIdFilter
* core/
  * domain/ -> Order aggregate, OrderStatus enum
    * validation/ -> ValidationConstants
  * application/
    * request/ -> Request DTOs: CreateOrderRequest, UpdateOrderStatusRequest
    * usecase/ -> CreateOrderUseCase, GetOrderUseCase, UpdateOrderStatusUseCase
    * mapper/ -> OrderMapper
    * event/ -> OrderCreatedEvent
  * infrastructure/
    * entity/ -> JPA OrderEntity
    * repository/ -> JPA OrderRepository interface
    * config/ -> ObjectMapperConfig, SwaggerConfig, RestClientConfig, RabbitMQConfig, RabbitMQProperties, CorrelationProperties
    * client/ -> UserValidationClient
    * publisher/ -> OrderEventPublisher

## Correlation ID
A `CorrelationIdFilter` reads or generates `X-Correlation-Id` on every request,
stores it in MDC, and returns it in the response header.
All outbound calls to user-service forward it automatically via a `RestClient` interceptor.
RabbitMQ events carry it as a message header.

### Resiliency
`UserValidationClient` uses Spring Retry — 3 attempts with 300ms backoff on `ResourceAccessException`.
- Returns `503 Service Unavailable` when user-service is unreachable after all retries.
- Returns `504 Gateway Timeout` when the cause is a `SocketTimeoutException`.
  HTTP connect timeout: 3s, read timeout: 5s (configured in `RestClientConfig`).

## Logging
Logback is configured in `src/main/resources/logback-spring.xml` to include `correlationId` from MDC in every log line:
```
%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} [correlationId=%X{correlationId}] - %msg%n
```
If no correlation ID is present in MDC (e.g. background threads), `%X{correlationId}` renders as empty string.

Two migration were applied, which encapsulated **orders** table creation and then its alteration (the removal of the customer name column and the addition of the customer id instead). It is located within _/src/main/resources/db/migration/V1__create_orders_table.sql_.
---

---
## How to Run Locally

**Prerequisites:** Java 17+, PostgreSQL running on `localhost:5432`
1. Create the database:
```sql
CREATE DATABASE cafetiria;
```

2. Set environment variables:
```bash
export DB_USER=postgres
export DB_PASSWORD=postgres
export DB_URL=jdbc:postgresql://localhost:5432/cafetiria
```

3. Run the application:
```bash
./mvnw spring-boot:run
```

The API will be available at `http://localhost:8088`.
--- 

---
## How to Run with Docker

**Prerequisites:** Docker and Docker Compose installed.

1. Create a `.env` file in the project root:
```env
DB_USER=postgres
DB_PASSWORD=postgres
```

2. Start all services:
```bash
docker compose up --build
```

3. To stop and remove containers:
```bash
docker compose down
```

4. To stop and also remove the database volume:
```bash
docker compose down -v
```

The API will be available at `http://localhost:8088`.
---

---
## How to Run Tests

**Run all tests:**
```bash
./mvnw test
```
```

---
## Swagger UI

Once the application is running, interactive API docs are available at:

```
http://localhost:8088/swagger-ui/index.html
```

---
# Health Check

Returns the application health status. Responds with `200 OK` when the app and database are reachable, or `503 Service Unavailable` when any component is down.

```bash
curl http://localhost:8088/api/health
```

**Response `200 UP`:**
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "diskSpace": { "status": "UP" }
  }
}
```

**Response `503 DOWN`:**
```json
{
  "status": "DOWN",
  "components": {
    "db": { "status": "DOWN" }
  }
}
```

---

---
## API Curl Examples

### Create an order

```bash
curl -X POST http://localhost:8088/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "ownerUserId": "b3f1c2d4-e5a6-7890-bcde-f12345678901",
    "itemName": "Latte",
    "quantity": 2,
    "price": 5.99
  }'
```

**Response `201 Created`:**
```json
{
  "id": "b3f1c2d4-e5a6-7890-bcde-f12345678901",
  "customerId": "b3f1c2d4-e5a6-7890-bcde-f12345678901",
  "itemName": "Latte",
  "quantity": 2,
  "price": 5.99,
  "status": "PENDING",
  "createdAt": "2024-03-01T10:00:00"
}
```

---

### Get an order by ID

```bash
curl http://localhost:8088/api/orders/b3f1c2d4-e5a6-7890-bcde-f12345678901
```

**Response `200 OK`:**
```json
{
  "id": "b3f1c2d4-e5a6-7890-bcde-f12345678901",
  "ownerUserId": "b3f1c2d4-e5a6-7890-bcde-f12345678901",
  "itemName": "Latte",
  "quantity": 2,
  "price": 5.99,
  "status": "PENDING",
  "createdAt": "2024-03-01T10:00:00"
}
```

---

### Update order status

**PENDING → CONFIRMED:**
```bash
curl -X PATCH http://localhost:8088/api/orders/b3f1c2d4-e5a6-7890-bcde-f12345678901/status \
  -H "Content-Type: application/json" \
  -d '{"newStatus": "CONFIRMED"}'
```

**CONFIRMED → DELIVERED:**
```bash
curl -X PATCH http://localhost:8088/api/orders/b3f1c2d4-e5a6-7890-bcde-f12345678901/status \
  -H "Content-Type: application/json" \
  -d '{"newStatus": "DELIVERED"}'
```

**PENDING → CANCELLED:**
```bash
curl -X PATCH http://localhost:8088/api/orders/b3f1c2d4-e5a6-7890-bcde-f12345678901/status \
  -H "Content-Type: application/json" \
  -d '{"newStatus": "CANCELLED"}'
```

**Response `204 No Content`** (empty body on success).

---

### Error examples

**Order not found `404`:**
```bash
curl http://localhost:8088/api/orders/00000000-0000-0000-0000-000000000000
```
```json
{"message": "Order with id: 00000000-0000-0000-0000-000000000000 was not found!"}
```

**Validation failure `400`:**
```bash
curl -X POST http://localhost:8088/api/orders \
  -H "Content-Type: application/json" \
  -d '{"ownerUserId": "", "itemName": "", "quantity": 0, "price": -1}'
```
```json
{"message": "customerName: must not be blank|\nitemName: must not be blank|\nquantity: must be greater than or equal to 1|\nprice: must be greater than 0"}
```

**Illegal status transition `400`:**
```bash
curl -X PATCH http://localhost:8088/api/orders/b3f1c2d4-e5a6-7890-bcde-f12345678901/status \
  -H "Content-Type: application/json" \
  -d '{"newStatus": "DELIVERED"}'
```
```json
{"message": "Cannot change order status from PENDING to DELIVERED"}
```

**Invalid UUID `400`:**
```bash
curl http://localhost:8088/orders/not-a-uuid
```
```json
{"message":"Invalid value for parameter 'id': not-a-uuid"}
```

**Invalid json format `400`:**
```bash
curl -X PATCH http://localhost:8088/api/orders/bd193441-2520-4392-a0aa-195000965fea/status \
  -H "Content-Type: application/json" \
  -d '{"newStatus": "CANCEL'    
```
```json
{"message":"Invalid JSON format in request body"}
```
---

## Messaging

RabbitMQ is used to publish events after successful order creation.

- **Exchange:** `order` (topic)
- **Routing key:** `order.created`
- **Queue:** `order.created`

### Event Payload Example

```json
{
  "eventId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "occurredAt": "2024-03-01T10:00:00Z",
  "correlationId": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
  "orderId": "c3d4e5f6-a7b8-9012-cdef-123456789012",
  "ownerUserId": "d4e5f6a7-b8c9-0123-defa-234567890123",
  "payload": "Order created: Latte x2 @ 5.99"
}
```

### Environment Variables

| Variable        | Default     | Description              |
|-----------------|-------------|--------------------------|
| `RABBITMQ_HOST` | `localhost` | RabbitMQ host            |
| `RABBIT_PORT`   | `5672`      | RabbitMQ AMQP port       |
| `RABBITMQ_USER` | `guest`     | RabbitMQ username        |
| `RABBITMQ_PASSWORD` | `guest`   | RabbitMQ password        |
```