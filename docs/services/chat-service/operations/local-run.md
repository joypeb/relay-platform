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

```bash
cd chat-service
./gradlew bootRun
```

## Test

```bash
cd chat-service
./gradlew test
```

테스트는 H2 in-memory DB와 Hibernate `create-drop`을 사용한다. 운영 DB schema는 `src/main/resources/db/migration/`의 Flyway migration으로 관리한다.
