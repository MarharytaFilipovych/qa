# Notification Service

Consumes order creation events from RabbitMQ and stores notification records in a dedicated PostgreSQL database.

---

## Architecture

src/main/java/com/microservices/margo/notification_service/:
* core/
  * application/
    * event/ -> OrderCreatedEvent
    * mapper/ -> NotificationMapper
    * usecase/ -> StoreNotificationUseCase
  * infrastructure/
    * config/ -> RabbitMQConfig, RabbitMQProperties, CorrelationProperties
    * entity/ -> JPA NotificationEntity
    * listener/ -> OrderCreatedListener
    * repository/ -> JPA NotificationRepository
    
## Correlation ID
The `OrderCreatedListener` reads `X-Correlation-Id` from the RabbitMQ message header
and stores it in MDC for the duration of event processing.
Falls back to `"unknown"` if the header is absent.

## Logging
Logback is configured in `src/main/resources/logback-spring.xml` to include `correlationId` from MDC in every log line:
```
%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} [correlationId=%X{correlationId}] - %msg%n
```
If the `X-Correlation-Id` header is absent from the RabbitMQ message, `correlationId` is set to `"unknown"` in MDC rather than empty string — this is an explicit fallback in `OrderCreatedListener` to make missing correlation IDs visible in logs rather than silent.

---

## Messaging

- **Exchange:** `order` (topic)
- **Queue:** `order.created`
- **Routing key:** `order.created`

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

---

## Domain Rules

- **Idempotency** — `event_id` has a unique constraint. Duplicate events are silently ignored.

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

4. To stop and also remove volumes:
```bash
docker compose down -v
```

The service will be available at `http://localhost:8090`.

---

## How to Verify

1. Create an order via the gateway:
```bash
curl -X POST http://localhost:8081/orders \
  -H "Content-Type: application/json" \
  -d '{
    "ownerUserId": "<valid-user-id>",
    "itemName": "Latte",
    "quantity": 2,
    "price": 5.99
  }'
```

2. Check the notification was stored:
```bash
docker exec -it notification-db psql -U postgres -d notifications \
  -c "SELECT * FROM notifications;"
```

---

## How to Run Tests

```bash
./mvnw test
```

## Environment Variables

| Variable          | Default     | Description            |
|-------------------|-------------|------------------------|
| `DB_URL`          | —           | JDBC datasource URL    |
| `DB_USER`         | —           | Database username      |
| `DB_PASSWORD`     | —           | Database password      |
| `RABBITMQ_HOST`   | `localhost` | RabbitMQ host          |
| `RABBIT_PORT`     | `5672`      | RabbitMQ AMQP port     |
| `RABBITMQ_USER`   | `guest`     | RabbitMQ username      |
| `RABBITMQ_PASSWORD` | `guest`     | RabbitMQ password      |
