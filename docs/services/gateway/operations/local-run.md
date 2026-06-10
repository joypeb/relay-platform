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
- `redis`: Redis 7, append-only file 활성화, `redis-data` 볼륨 사용
- `postgres`: PostgreSQL 17, `postgres-data` 볼륨 사용

기본 외부 포트:

- gateway: `8080`
- Redis: `6379`
- PostgreSQL: `5432`

gateway 컨테이너에는 다음 Spring 환경 변수가 주입된다.

- `SERVER_PORT`
- `SPRING_PROFILES_ACTIVE`
- `SPRING_DATA_REDIS_HOST`
- `SPRING_DATA_REDIS_PORT`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

헬스체크:

- gateway: `GET /actuator/health`
- Redis: `redis-cli ping`
- PostgreSQL: `pg_isready`

### Run Gateway Only

```bash
cd gateway
./gradlew bootRun
```

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

- Redis: `docker compose logs redis`, `docker compose exec redis redis-cli ping`
- PostgreSQL: `docker compose logs postgres`, `docker compose exec postgres pg_isready -U redis_chat -d redis_chat`
