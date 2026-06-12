# chat-service Local Run

## Prerequisites

- Java 21
- PostgreSQL 17 또는 `docker-compose.yml`의 `postgres` 서비스
- Redis 7 또는 `docker-compose.yml`의 `redis` 서비스

## Environment

기본 설정은 다음 환경 변수를 사용한다.

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/redis_chat
SPRING_DATASOURCE_USERNAME=redis_chat
SPRING_DATASOURCE_PASSWORD=redis_chat_password
SPRING_DATA_REDIS_HOST=localhost
SPRING_DATA_REDIS_PORT=6379
CHAT_READ_STATE_FLUSH_INTERVAL_MS=30000
CHAT_READ_STATE_FLUSH_BATCH_SIZE=500
```

## Run

### Run With Docker Compose

루트의 `.env.example`을 `.env`로 복사한 뒤 필요한 포트와 계정 값을 조정한다.

```bash
cp .env.example .env
docker compose up --build chat-service postgres redis
```

기본 compose 설정은 다음 값을 사용한다.

- image: `redis-chat-test-chat-service:${CHAT_SERVICE_IMAGE_TAG:-local}`
- host port: `${CHAT_SERVICE_HOST_PORT:-8081}`
- container port: `${CHAT_SERVICE_CONTAINER_PORT:-8081}`
- datasource URL: `jdbc:postgresql://postgres:${POSTGRES_PORT:-5432}/${POSTGRES_DB:-redis_chat}`
- Redis host: `redis`
- Redis port: `${REDIS_PORT:-6379}`
- healthcheck: `GET /actuator/health`

### Run With Gradle

```bash
cd chat-service
SERVER_PORT=8081 ./gradlew bootRun
```

## Test

```bash
cd chat-service
./gradlew test
```

테스트는 H2 in-memory DB와 Hibernate `create-drop`을 사용한다. 운영 DB schema는 `src/main/resources/db/migration/`의 Flyway migration으로 관리한다.
Spring Boot 4에서는 `spring-boot-starter-flyway`가 Flyway 자동 설정을 제공하며, 운영 실행 시 migration이 JPA schema validation보다 먼저 적용되어야 한다.

## Read State Scheduler

채팅 읽음 상태는 Redis에 즉시 반영되고 `chat.read-state.flush-interval-ms` 간격으로 PostgreSQL `chat_room_read_states`에 flush된다.

- 기본 flush interval: `30000ms`
- 기본 batch size: `500`
- dirty set key: `chat:read-state:dirty`
- Redis 장애 또는 애플리케이션 재시작 전 미flush 상태는 최대 30초 범위에서 DB에 반영되지 않을 수 있다.
