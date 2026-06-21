# User Service

**User Service** is a microservice responsible for managing cafeteria users.

- **Core Item:** `User`
- **Core Action:** `CreateUser`

---

## Domain Rules

1. **Name cannot be blank**
2. **Surname cannot be blank**
3. **Surname and name must contain at most 255 symbols"
3. **Phone must be 7–20 digits and may include +, spaces, dashes, or parentheses** (optional field)
4. **Birth date must meet minimum age requirement and is required** — enforced via `@MinAge` custom annotation
5. **Email must be valid and cannot be blank and can contain at most 100 symbols**

---

## Architecture

src/main/java/com/microservices/margo/users/
* api/ -> HTTP layer (no business logic): UserController
  * exception/ -> ErrorResponse, GlobalExceptionHandler
  * filter/ -> CorrelationIdFilter
* core/
  * domain/ -> User record
    * validation/ -> MinAge, MimAgeValidator, ValidationConstants
  * application/
    * request/ -> Request DTOs: CreateUserRequest
    * usecase/ -> CreateUserUseCase, GetUserUseCase
    * mapper/ -> UserMapper
* infrastructure/
  * entity/ -> JPA UserEntity
  * repository/ -> JPA UserRepository interface
  * config/ -> ObjectMapperConfig, SwaggerConfig


One migration was applied which created the **users** table. It is located within
`/src/main/resources/db/migration/V1__create_users_table.sql`.


## Correlation ID
A `CorrelationIdFilter` reads or generates `X-Correlation-Id` on every request,
stores it in MDC, and returns it in the response header.

## Logging
Logback is configured in `src/main/resources/logback-spring.xml` to include `correlationId` from MDC in every log line:
```
%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} [correlationId=%X{correlationId}] - %msg%n
```
If no correlation ID is present in MDC (e.g. background threads), `%X{correlationId}` renders as empty string.

---

## How to Run Locally

**Prerequisites:** Java 21+, PostgreSQL running on `localhost:5433`

1. Create the database:

```sql
CREATE DATABASE users;
```

2. Set environment variables:

```bash
export DB_USER=postgres
export DB_PASSWORD=postgres
export DB_URL=jdbc:postgresql://localhost:5433/users
```

3. Run the application:

```bash
./mvnw spring-boot:run
```

The API will be available at `http://localhost:8080`.

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

The API will be available at `http://localhost:8080`.

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
http://localhost:8080/swagger-ui/index.html
```

---

## Health Check

Returns the application health status. Responds with `200 OK` when the app and database
are reachable, or `503 Service Unavailable` when any component is down.

```bash
curl http://localhost:8080/api/health
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

### Create a user

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Marharyta",
    "surname": "Kovalenko",
    "phone": "+380991234567",
    "birthDate": "2000-01-15",
    "email": "marharyta@example.com"
  }'
```

**Response `201 Created`:**

```json
{
  "id": "b3f1c2d4-e5a6-7890-bcde-f12345678901",
  "name": "Marharyta",
  "surname": "Kovalenko",
  "phone": "+380991234567",
  "birthDate": "2000-01-15",
  "email": "marharyta@example.com",
  "createdAt": "2024-03-01T10:00:00"
}
```

---

### Get a user by ID

```bash
curl http://localhost:8080/api/users/b3f1c2d4-e5a6-7890-bcde-f12345678901
```

**Response `200 OK`:**

```json
{
  "id": "b3f1c2d4-e5a6-7890-bcde-f12345678901",
  "name": "Marharyta",
  "surname": "Kovalenko",
  "phone": "+380991234567",
  "birthDate": "2000-01-15",
  "email": "marharyta@example.com",
  "createdAt": "2024-03-01T10:00:00"
}
```

---

### Error Examples

**User not found `404`:**

```bash
curl http://localhost:8080/api/users/00000000-0000-0000-0000-000000000000
```

```json
{"message": "User not found: 00000000-0000-0000-0000-000000000000"}
```

---

**Validation failure `400`:**

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name": "", "surname": "", "birthDate": null, "email": "not-an-email"}'
```

```json
{"message": "name: Name must not be blank | surname: Surname must not be blank | email: must be a well-formed email address"}
```

---

**Invalid phone format `400`:**

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Marharyta",
    "surname": "Kovalenko",
    "phone": "123",
    "birthDate": "2000-01-15",
    "email": "marharyta@example.com"
  }'
```

```json
{"message": "phone: Phone number must be 7–20 digits and may include +, spaces, dashes, or parentheses"}
```

---

**User too young `400`:**

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Marharyta",
    "surname": "Kovalenko",
    "birthDate": "2020-01-15",
    "email": "marharyta@example.com"
  }'
```

```json
{"message": "birthDate: User must be at least 14 years old"}
```

---

**Invalid UUID `400`:**

```bash
curl http://localhost:8080/api/users/not-a-uuid
```

```json
{"message": "Invalid value for parameter 'id': not-a-uuid"}
```

---

**Invalid JSON format `400`:**

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name": "Marharyta"'
```

```json
{"message": "Invalid JSON format in request body"}
```