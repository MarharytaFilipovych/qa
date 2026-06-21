# Workflow Service

**Workflow Service** orchestrates the *Create Order* saga for the Cafetiria distributed system.  
It drives the full lifecycle: create order → confirm order → compensate on failure, and persists each step as a `WorkflowState` in its own database.

---

## Architecture

src/main/java/com/microservices/margo/workflow_service/
* api/ -> WorkflowController, HealthController
    * exception/ -> GlobalExceptionHandler, ErrorResponse
    * filter/ -> CorrelationIdFilter
* core/
    * application/
        * mapper/ -> WorkflowMapper
        * request/ -> CreateOrderRequest
        * usecase/ -> StartCreateOrderWorkflowUseCase, GetWorkflowUseCase
    * domain/ -> Workflow (domain record), WorkflowState (STARTED, ORDER_CREATED, ORDER_CONFIRMED, COMPLETED, COMPENSATED, FAILED), WorkflowType (ORDER_CREATED), OrderStatus
      * validation/ -> ValidationConstants
    * infrastructure/
        * client/ -> OrderServiceClient
        * entity/ -> WorkflowEntity
        * repository/ -> WorkflowRepository
        * config/ -> SwaggerConfig, RestClientConfig, ObjectMapperConfig, OrderServiceProperties, CorrelationProperties
      
### Saga steps

1) Create order via Order Service  → creation failed -> `FAILED` 
2) Confirm order (`PENDING → CONFIRMED`) → confirmation failed -> cancel order (`PENDING → CANCELLED`) → `COMPENSATED` (cancellation failed) or `FAILED` (compensation failed)
3) Mark workflow `COMPLETED` (if everything worked out)

### Correlation ID
A `CorrelationIdFilter` reads or generates `X-Correlation-Id` on every request
and stores it in MDC. Outbound calls to order-service forward it via a `RestClient` interceptor.

## Resiliency
`OrderServiceClient` uses Spring Retry — 3 attempts with 300ms backoff on `ResourceAccessException`.
- Returns `503 Service Unavailable` when order-service is unreachable after all retries.
- Returns `504 Gateway Timeout` when the cause is a `SocketTimeoutException`.
  HTTP connect timeout: 3s, read timeout: 5s (configured in `RestClientConfig`).

### Logging
Logback is configured in `src/main/resources/logback-spring.xml` to include `correlationId` from MDC in every log line:
```
%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} [correlationId=%X{correlationId}] - %msg%n
```
If no correlation ID is present in MDC (e.g. background threads), `%X{correlationId}` renders as empty string.

---

## How to Run Locally

**Prerequisites:** Java 17+, Order Service running, PostgreSQL running.

1. Set environment variables:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/workflow_db
export DB_USER=postgres
export DB_PASSWORD=postgres
export ORDER_SERVICE_URL=http://localhost:8088
```

2. Run the application:

```bash
./mvnw spring-boot:run
```

The API will be available at `http://localhost:8092`.

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

The API will be available at `http://localhost:8091`.

---

## How to Run Tests

**Run all tests:**

```bash
./mvnw test
```
---

## Swagger UI

Once the application is running, interactive API docs are available at:

```
http://localhost:8091/swagger-ui/index.html
```

---

## Health Check

Responds with `200 OK` when the app and database are reachable, or `503 Service Unavailable` when any component is down.

```bash
curl http://localhost:8091/api/health
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

## API Curl Examples

### Start a workflow (Create Order saga)

```bash
curl -X POST http://localhost:8091/api/workflows \
  -H "Content-Type: application/json" \
  -d '{
    "ownerUserId": "b3f1c2d4-e5a6-7890-bcde-f12345678901",
    "itemName": "Latte",
    "quantity": 2,
    "price": 5.99
  }'
```

Here is the corrected section:

### Start a workflow (Create Order saga)

```bash
curl -X POST http://localhost:8091/api/workflows \
  -H "Content-Type: application/json" \
  -d '{
    "ownerUserId": "b3f1c2d4-e5a6-7890-bcde-f12345678901",
    "itemName": "Latte",
    "quantity": 2,
    "price": 5.99
  }'
```

**Response `201 Created` — happy path:**
```json
{
  "id": "28f581fb-0352-45c9-b248-3c92c09c07a7",
  "type": "CREATE_ORDER",
  "state": "COMPLETED",
  "payload": "{\"ownerUserId\":\"b3f1c2d4-e5a6-7890-bcde-f12345678901\",\"itemName\":\"Latte\",\"quantity\":2,\"price\":5.99}",
  "lastError": null,
  "createdAt": "2024-03-01T10:00:00",
  "updatedAt": "2024-03-01T10:00:01"
}
```

**Response `201 Created` — compensation succeeded:**
```json
{
  "id": "28f581fb-0352-45c9-b248-3c92c09c07a7",
  "type": "CREATE_ORDER",
  "state": "COMPENSATED",
  "payload": "{\"ownerUserId\":\"b3f1c2d4-e5a6-7890-bcde-f12345678901\",\"itemName\":\"Latte\",\"quantity\":2,\"price\":5.99}",
  "lastError": "Confirm failed: 503 SERVICE_UNAVAILABLE",
  "createdAt": "2024-03-01T10:00:00",
  "updatedAt": "2024-03-01T10:00:01"
}
```

**Response `201 Created` — compensation also failed:**
```json
{
  "id": "28f581fb-0352-45c9-b248-3c92c09c07a7",
  "type": "CREATE_ORDER",
  "state": "FAILED",
  "payload": "{\"ownerUserId\":\"b3f1c2d4-e5a6-7890-bcde-f12345678901\",\"itemName\":\"Latte\",\"quantity\":2,\"price\":5.99}",
  "lastError": "Confirm failed: 503 SERVICE_UNAVAILABLE; cancel also failed",
  "createdAt": "2024-03-01T10:00:00",
  "updatedAt": "2024-03-01T10:00:01"
}
```

---

### Get a workflow by ID

```bash
curl http://localhost:8091/api/workflows/28f581fb-0352-45c9-b248-3c92c09c07a7
```

**Response `200 OK`:**
```json
{
  "id": "28f581fb-0352-45c9-b248-3c92c09c07a7",
  "type": "CREATE_ORDER",
  "state": "COMPLETED",
  "payload": "{\"ownerUserId\":\"b3f1c2d4-e5a6-7890-bcde-f12345678901\",\"itemName\":\"Latte\",\"quantity\":2,\"price\":5.99}",
  "lastError": null,
  "createdAt": "2024-03-01T10:00:00",
  "updatedAt": "2024-03-01T10:00:01"
}
```
---

### Error examples

**Workflow not found `404`:**
```bash
curl http://localhost:8091/api/workflows/00000000-0000-0000-0000-000000000000
```
```json
{"message": "Workflow with id: 00000000-0000-0000-0000-000000000000 was not found!"}
```

**Validation failure `400`:**
```bash
curl -X POST http://localhost:8091/api/workflows \
  -H "Content-Type: application/json" \
  -d '{"ownerUserId": null, "itemName": "", "quantity": 0, "price": -1}'
```
```json
{"message": "ownerUserId: must not be null | itemName: must not be blank | quantity: must be greater than or equal to 1 | price: must be greater than 0"}
```

**Invalid UUID `400`:**
```bash
curl http://localhost:8091/api/workflows/not-a-uuid
```
```json
{"message": "Invalid value for parameter 'id': not-a-uuid"}
```

**Invalid JSON format `400`:**
```bash
curl -X POST http://localhost:8091/api/workflows \
  -H "Content-Type: application/json" \
  -d '{"ownerUserId": "b3f1c2d4'
```
```json
{"message": "Invalid JSON format in request body"}
```

---

## Workflow State Machine

1) STARTED 
2) ORDER_CREATED -> order creation fails -> FAILED
3) ORDER_CONFIRMED -> confirm fails -> COMPENSATED  (cancel succeeded) or FAILED (cancel also failed)
4) COMPLETED
                        ↘
