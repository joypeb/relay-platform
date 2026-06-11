# Gateway Local Run

gateway 서비스 로컬 실행과 검증 절차를 기록하는 문서다.

## Prerequisites

- Java: 21
- Gradle: gateway Gradle wrapper
- Docker: Docker Compose v2
- Redis: Docker Compose `redis:7`
- PostgreSQL: Docker Compose `postgres:17`

## Run

### Run With Docker Compose

루트의 `.env.example`을 `.env`로 복사한 뒤 필요한 포트와 계정 값을 조정한다.

```bash
cp .env.example .env
docker compose up --build
```

구성 요소:

- `gateway`: `gateway/Dockerfile`로 빌드되는 Spring Boot gateway 컨테이너
- `chat-service`: `chat-service/Dockerfile`로 빌드되는 Spring Boot chat-service 컨테이너
- `redis`: Redis 7, append-only file 활성화, `redis-data` 볼륨 사용
- `postgres`: PostgreSQL 17, `postgres-data` 볼륨 사용

기본 외부 포트:

- gateway: `8080`
- chat-service: `8081`
- chat-service base URL: `http://chat-service:8081`
- Redis: `6379`
- PostgreSQL: `5432`

gateway 컨테이너에는 다음 Spring 환경 변수가 주입된다.

- `SERVER_PORT`
- `SPRING_PROFILES_ACTIVE`
- `CHAT_SERVICE_BASE_URL`
- `SPRING_DATA_REDIS_HOST`
- `SPRING_DATA_REDIS_PORT`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

헬스체크:

- gateway: `GET /actuator/health`
- chat-service: `GET /actuator/health`
- Redis: `redis-cli ping`
- PostgreSQL: `pg_isready`

### Run Gateway Only

```bash
cd gateway
CHAT_SERVICE_BASE_URL=http://localhost:8081 ./gradlew bootRun
```

`CHAT_SERVICE_BASE_URL`을 지정하지 않으면 gateway는 기본적으로 `http://localhost:8081`의 chat-service를 호출한다.

### Run Browser Test HTML

프로젝트 루트의 `chat-test.html`을 IDE preview server나 간단한 정적 서버로 열어 gateway `http://localhost:8080`을 호출할 수 있다.

gateway의 REST CORS 기본값은 local browser tooling을 위해 다음 origin pattern을 허용한다.

- `http://localhost:*`
- `http://127.0.0.1:*`

운영 환경이나 공유 개발 환경에서는 `gateway.cors.allowed-origin-patterns`를 신뢰할 수 있는 origin으로 제한한다.

## Test

```bash
cd gateway
./gradlew test
```

## Troubleshooting

### Application Fails To Start

- 확인할 설정: `gateway/src/main/resources/application.yaml`
- 확인할 로그: `docker compose logs gateway`

### External Dependency Connection Fails

- chat-service: `CHAT_SERVICE_BASE_URL`이 실행 중인 chat-service 주소와 일치하는지 확인한다.
- Redis: `docker compose logs redis`, `docker compose exec redis redis-cli ping`
- PostgreSQL: `docker compose logs postgres`, `docker compose exec postgres pg_isready -U redis_chat -d redis_chat`

### Browser Test HTML Fails To Fetch

- gateway가 `http://localhost:8080`에서 실행 중인지 확인한다.
- `chat-test.html`의 Gateway URL 값이 실제 gateway 주소와 일치하는지 확인한다.
- 브라우저 개발자 도구에서 CORS 오류가 보이면 `gateway.cors.allowed-origin-patterns`에 HTML을 제공하는 origin이 포함되어 있는지 확인한다.
