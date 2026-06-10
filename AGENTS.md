# Codex Project Instructions

## 기본 응답

- 이 파일은 Codex가 프로젝트 규칙을 필요한 상황에만 선택적으로 참고하기 위한 라우팅 문서다.
- 작업과 직접 관련 없는 rule 파일은 읽거나 적용하지 않는다.
- 여러 rule이 동시에 관련되면 더 구체적인 rule을 우선하고, 충돌 시 해당 도메인 rule을 `00-project-common.md`보다 우선한다.

## 공통 규칙

다음 상황에서는 `.agent/rules/00-project-common.md`를 참고한다.

- Java 21, Spring Boot 4, Spring Web MVC, JPA, PostgreSQL, WebSocket, STOMP, Redis, Redis Streams, MSA 전반에 걸친 변경을 할 때
- 새 Spring Boot 애플리케이션, 모듈, 패키지 구조, 설정, 의존성 주입 방식을 정할 때
- DTO, Entity 노출, 운영 로그, metrics, tracing, health check 같은 프로젝트 공통 기준을 점검할 때
- 어떤 세부 rule을 적용해야 할지 애매한 기본 설계 판단이 필요할 때

## 기능 완료 문서화

기능 추가, 기능 변경, 비즈니스 로직 변경, API/이벤트/Redis/JPA 계약 변경이 포함된 작업을 완료할 때는 반드시 관련 문서를 작성하거나 갱신한다.

문서 위치 기준:

- 서비스별 기능 설명: `docs/services/<service-name>/features/<feature-slug>.md`
- 서비스별 내부 구조: `docs/services/<service-name>/architecture/*.md`
- 서비스별 실행, 운영, 장애 대응: `docs/services/<service-name>/operations/*.md`
- 서비스별 설계 결정: `docs/services/<service-name>/adr/YYYY-MM-DD-<decision-slug>.md`
- 여러 서비스에 걸친 아키텍처/서비스 경계 변경: `docs/architecture/*.md`
- 여러 서비스에 영향을 주는 중요한 설계 결정: `docs/adr/YYYY-MM-DD-<decision-slug>.md`

문서에는 최소한 다음 내용을 포함한다.

1. 기능 목적
2. 요청 또는 이벤트의 전체 처리 흐름
3. 핵심 로직과 주요 분기
4. 관련 코드 파일 경로
5. API, WebSocket, Redis, DB 등 외부 계약
6. 실패 처리와 예외 상황
7. 테스트 및 검증 방법
8. 중요한 설계 결정과 trade-off

작업 완료 전 체크리스트:

- 코드 변경과 문서 내용이 일치하는지 확인한다.
- 새 기능이면 해당 서비스의 `docs/services/<service-name>/features/`에 문서를 추가한다.
- 기존 기능 변경이면 기존 문서를 갱신한다.
- 변경이 여러 서비스에 걸치면 각 서비스 문서를 갱신하고, 서비스 간 흐름은 `docs/architecture/` 또는 전역 ADR에 별도로 남긴다.
- 최종 응답에 작성/수정한 문서 경로를 포함한다.
- 문서화가 불필요한 단순 변경이면 그 이유를 최종 응답에 짧게 밝힌다.

## 상황별 Rule 라우팅

### 아키텍처와 MSA 경계

`.agent/rules/01-architecture.md`는 다음 상황에서만 참고한다.

- microservice 경계, 데이터 소유권, 서비스 간 의존 방향을 설계하거나 변경할 때
- 서비스 간 REST 호출, event, message, read model 접근 방식을 정할 때
- gateway, cross-service transaction, eventual consistency, outbox, compensating action을 다룰 때
- timeout, retry, circuit breaker, fallback, distributed tracing 전파를 설계하거나 검토할 때

### REST API

`.agent/rules/02-rest-api.md`는 다음 상황에서만 참고한다.

- Spring Web MVC Controller, REST endpoint, URL, HTTP method를 추가하거나 수정할 때
- request/response DTO, pagination, validation, status code를 설계하거나 검토할 때
- REST error response, `ProblemDetail`, `@RestControllerAdvice`를 구현하거나 수정할 때
- public API prefix, resource naming, REST contract 변경 여부를 판단할 때

### WebSocket과 STOMP

`.agent/rules/03-websocket-stomp.md`는 다음 상황에서만 참고한다.

- WebSocket handshake endpoint, STOMP destination, `@MessageMapping`을 추가하거나 수정할 때
- 실시간 채팅, broadcast, user-specific queue, ack, notification, error channel을 다룰 때
- STOMP payload, server event payload, requestId/eventId/traceId 계약을 설계할 때
- WebSocket 인증, room 권한, subscription/send 권한, 재접속 복구 흐름을 구현하거나 검토할 때

### Redis와 Redis Streams

`.agent/rules/04-redis-stream.md`는 다음 상황에서만 참고한다.

- Redis key, TTL, serializer, cache, session, presence, lock, dedupe 저장소를 설계하거나 변경할 때
- Redis Streams key, event schema, consumer group, consumer name을 추가하거나 수정할 때
- stream consumer의 ACK, pending entry, retry, DLQ, idempotency를 구현하거나 검토할 때
- DB 저장과 Redis publish 사이의 정합성, outbox, 실패 보상 전략을 다룰 때

### JPA와 PostgreSQL

`.agent/rules/05-jpa-postgresql.md`는 다음 상황에서만 참고한다.

- JPA Entity, aggregate, repository, query, projection을 추가하거나 수정할 때
- transaction boundary, `@Transactional`, read-only transaction, locking, concurrency를 다룰 때
- PostgreSQL table, column, index, constraint, migration, naming을 설계하거나 변경할 때
- N+1, lazy loading, Open Session in View, DTO projection, schema integrity를 검토할 때

### 코드 리뷰와 리팩토링

`.agent/rules/06-code-review-refactoring.md`는 다음 상황에서만 참고한다.

- 코드 리뷰, 리팩토링, 이름 변경, 중복 제거, 추상화 판단을 수행할 때
- logging, tracing, metrics, error-prone code를 점검하거나 개선할 때
- design pattern, interface 도입, domain model, value object 설계를 검토할 때
- 실패 처리, broad catch, null 처리, 시간/상태/money/count 모델링을 점검할 때

### 테스트

`.agent/rules/07-testing.md`는 다음 상황에서만 참고한다.

- 테스트를 작성, 수정, 삭제하거나 테스트 전략을 제안할 때
- bug fix regression test, 신규 business rule unit test, 외부 연동 integration test를 다룰 때
- `@WebMvcTest`, `@DataJpaTest`, `@SpringBootTest`, Testcontainers, MockMvc, Awaitility 사용을 판단할 때
- REST, WebSocket/STOMP, Redis Stream, JPA/PostgreSQL, contract test의 검증 범위를 정할 때

## 적용 절차

1. 먼저 현재 요청이 위 상황 중 어디에 해당하는지 판단한다.
2. 해당하는 rule 파일만 읽고 적용한다.
3. 여러 상황에 걸치면 관련 rule 파일만 조합해 참고한다.
4. 작업 범위 밖의 rule은 추측으로 적용하지 않는다.
5. rule 적용 여부가 결과에 영향을 주면, 최종 응답에 어떤 rule을 참고했는지 간단히 언급한다.
