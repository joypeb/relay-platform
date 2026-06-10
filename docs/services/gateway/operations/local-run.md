# Gateway Local Run

gateway 서비스 로컬 실행과 검증 절차를 기록하는 문서다.

## Prerequisites

- Java:
- Gradle:
- Docker:
- Redis:
- PostgreSQL:

## Run

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
- 확인할 로그:

### External Dependency Connection Fails

- Redis:
- PostgreSQL:
