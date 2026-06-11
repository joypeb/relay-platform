# chat-service Local Run

## Prerequisites

- Java 21
- PostgreSQL 17 또는 `docker-compose.yml`의 `postgres` 서비스

## Environment

기본 설정은 다음 환경 변수를 사용한다.

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/redis_chat
SPRING_DATASOURCE_USERNAME=redis_chat
SPRING_DATASOURCE_PASSWORD=redis_chat_password
```

## Run

### Run With Docker Compose

루트의 `.env.example`을 `.env`로 복사한 뒤 필요한 포트와 계정 값을 조정한다.

```bash
cp .env.example .env
docker compose up --build chat-service postgres
```

기본 compose 설정은 다음 값을 사용한다.

- image: `redis-chat-test-chat-service:${CHAT_SERVICE_IMAGE_TAG:-local}`
- host port: `${CHAT_SERVICE_HOST_PORT:-8081}`
- container port: `${CHAT_SERVICE_CONTAINER_PORT:-8081}`
- datasource URL: `jdbc:postgresql://postgres:${POSTGRES_PORT:-5432}/${POSTGRES_DB:-redis_chat}`
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
