# Relay Platform

`relay-platform` is a Spring Boot based chat platform project. It currently separates the gateway and chat domain into independently runnable services, and uses Docker Compose to run PostgreSQL, Redis, and both Spring Boot applications together.

## Project Structure

```text
relay-platform
├── gateway       # Session authentication, REST routing, WebSocket/STOMP entry point
├── chat-service  # Chat room domain API and PostgreSQL-owned source data
├── docs          # Service feature, operations, and architecture documents
└── docker-compose.yml
```

| Component | Default Port | Responsibility |
| --- | ---: | --- |
| gateway | 8080 | Client entry point, local sessions, chat-service REST routing, STOMP handshake |
| chat-service | 8081 | Chat room creation, lookup, update, and deletion |
| PostgreSQL | 5432 | Chat room and membership persistence |
| Redis | 6379 | Infrastructure reserved for later real-time chat, presence, and stream processing |

## Tech Stack

- Java 21
- Spring Boot 4.0.6
- Spring Web MVC
- Spring Cloud Gateway Server MVC
- Spring WebSocket / STOMP
- Spring Data JPA
- Flyway
- PostgreSQL 17
- Redis 7
- Gradle Wrapper
- Docker Compose v2

## Quick Start

### 1. Create the environment file

```bash
cp .env.example .env
```

No changes are required if the default ports are available on your machine.

### 2. Start all services

```bash
docker compose up --build
```

Docker Compose starts the services using the following health checks:

- PostgreSQL: `pg_isready`
- Redis: `redis-cli ping`
- chat-service: `GET /actuator/health`
- gateway: `GET /actuator/health`

### 3. Verify the services

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
```

## API Examples

### Create a local session

The gateway uses a temporary `userId` based local session until a real authentication service is introduced.

```bash
curl -i -c cookies.txt \
  -H 'Content-Type: application/json' \
  -d '{"userId":"user-1"}' \
  http://localhost:8080/api/v1/sessions
```

### Get the current session

```bash
curl -b cookies.txt http://localhost:8080/api/v1/sessions/current
```

### Create a chat room

Clients only send the gateway session cookie. The gateway converts the session user ID into the downstream `X-User-Id` header for chat-service.

```bash
curl -i -b cookies.txt \
  -H 'Content-Type: application/json' \
  -d '{"name":"general","description":"General discussion","publiclyVisible":true}' \
  http://localhost:8080/api/v1/chat-rooms
```

### List public chat rooms

```bash
curl -b cookies.txt \
  'http://localhost:8080/api/v1/chat-rooms?scope=public&page=0&size=20'
```

## Main Contracts

### Gateway REST

- `POST /api/v1/sessions`
- `GET /api/v1/sessions/current`
- `DELETE /api/v1/sessions/current`
- `POST /api/v1/chat-rooms`
- `GET /api/v1/chat-rooms`
- `GET /api/v1/chat-rooms/{roomId}`
- `PATCH /api/v1/chat-rooms/{roomId}`
- `DELETE /api/v1/chat-rooms/{roomId}`

### WebSocket/STOMP

- Handshake endpoint: `/ws`
- Client publish prefix: `/app`
- Broadcast subscription prefix: `/topic`
- User subscription prefix: `/user/queue`
- Connection verification:
  - `SEND /app/gateway/acks`
  - `SUBSCRIBE /user/queue/gateway/acks`

### Chat Service REST

chat-service runs as an internal service and uses the `X-User-Id` header forwarded by the gateway as the actor identifier.

- `POST /api/v1/chat-rooms`
- `GET /api/v1/chat-rooms?scope=public|joined&page=0&size=20`
- `GET /api/v1/chat-rooms/{roomId}`
- `PATCH /api/v1/chat-rooms/{roomId}`
- `DELETE /api/v1/chat-rooms/{roomId}`

## Local Development

### Run only gateway

chat-service must already be running on `localhost:8081`.

```bash
cd gateway
CHAT_SERVICE_BASE_URL=http://localhost:8081 ./gradlew bootRun
```

### Run only chat-service

PostgreSQL must already be running. The default datasource settings match `.env.example`.

```bash
cd chat-service
SERVER_PORT=8081 ./gradlew bootRun
```

## Tests

Each service is a separate Gradle project.

```bash
cd gateway
./gradlew test
```

```bash
cd chat-service
./gradlew test
```

chat-service tests use an H2 in-memory database. The production schema is managed through Flyway migrations under `chat-service/src/main/resources/db/migration/`.

## Documentation

- System architecture: `docs/architecture/overview.md`
- Documentation rules: `docs/README.md`
- gateway documents: `docs/services/gateway/README.md`
- gateway local run guide: `docs/services/gateway/operations/local-run.md`
- gateway feature guide: `docs/services/gateway/features/local-session-and-stomp-gateway.md`
- chat-service documents: `docs/services/chat-service/README.md`
- chat-service local run guide: `docs/services/chat-service/operations/local-run.md`
- chat-service chat room feature guide: `docs/services/chat-service/features/chat-rooms.md`

## Operations Notes

- Application configuration is externalized through `application.yaml` and environment variables.
- The gateway does not trust client-provided `X-User-Id` values. It overwrites them with the authenticated session user ID.
- The default WebSocket allowed origin pattern is `*` for local development. Production environments should restrict it to explicit origin patterns.
- Redis is included in the Compose infrastructure but is not used by chat room CRUD yet. Message delivery, presence, and Redis Stream processing should be documented as separate contracts when those features are added.
