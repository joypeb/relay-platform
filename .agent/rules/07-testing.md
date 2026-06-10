# Testing Rules

## Scope

- 이 규칙은 TDD, unit test, integration test, contract test, WebSocket/STOMP test, Redis/PostgreSQL test 기준을 정의한다.
- 테스트는 구현 세부사항이 아니라 observable behavior와 contract를 검증해야 한다.

## TDD

- 버그 수정은 실패하는 regression test를 먼저 작성한 뒤 수정한다.
- 신규 business rule은 단위 테스트를 먼저 작성하거나, 최소한 구현 PR 안에 같은 scope의 단위 테스트를 포함한다.
- 외부 시스템 연동 기능은 단위 테스트로 adapter contract를 고정하고 통합 테스트로 실제 연동을 검증한다.

## Unit Test

- domain model, domain service, application service의 분기, 경계값, 예외를 테스트한다.
- unit test는 Spring context를 띄우지 않는다.
- 외부 의존성은 fake, stub, mock 중 하나로 격리한다.
- test method 이름은 조건, 행위, 기대 결과를 드러내야 한다.
- time-dependent code는 `Clock`을 주입해 고정한다.

## Spring Test Slice

- REST controller는 `@WebMvcTest` 또는 MockMvc 기반 slice test를 우선 사용한다.
- JPA repository는 `@DataJpaTest`를 사용하고 실제 PostgreSQL 차이는 Testcontainers 통합 테스트로 보완한다.
- Redis adapter는 Spring Data Redis 연동을 포함한 integration test를 작성한다.
- 전체 context가 필요한 경우에만 `@SpringBootTest`를 사용한다.

## Integration Test

- PostgreSQL, Redis, broker relay 등 외부 인프라는 Testcontainers를 우선 사용한다.
- Spring Boot service connection 또는 `@DynamicPropertySource`로 container endpoint를 주입한다.
- migration이 있으면 integration test 시작 시 migration이 적용되어야 한다.
- transaction, constraint, index-dependent behavior는 실제 PostgreSQL로 검증한다.
- Redis Stream consumer는 publish, read, ack, retry, DLQ 흐름을 검증한다.

## REST API Test

- status code, response body, error body, validation error, authorization failure를 검증한다.
- 성공 path만 테스트하지 않는다.
- pagination API는 default size, max size, sort, empty result를 검증한다.

## WebSocket and STOMP Test

- connect, subscribe, send, receive 흐름을 검증한다.
- 인증 실패, 권한 없는 room subscribe/send 실패를 검증한다.
- server event에 `eventId`, `type`, `occurredAt`, `traceId`가 포함되는지 검증한다.
- 비동기 대기는 fixed sleep이 아니라 Awaitility 같은 조건 기반 대기를 사용한다.

## Contract and MSA Test

- service-to-service REST API는 consumer-driven contract test를 검토한다.
- event schema는 version과 required field를 테스트한다.
- breaking change는 contract test 실패로 드러나야 한다.

## Test Data

- 테스트 데이터는 builder, mother object, fixture factory 중 하나로 생성한다.
- 각 테스트는 독립 실행 가능해야 한다.
- 테스트 간 DB/Redis 상태 공유를 금지한다.
- 테스트가 순서에 의존하지 않아야 한다.

## 금지

- 단순 coverage 수치를 위해 의미 없는 getter/setter test를 작성하지 않는다.
- `Thread.sleep`으로 비동기 처리를 기다리지 않는다.
- 운영 DB, 운영 Redis, 개발자 로컬 고정 포트에 의존하지 않는다.
- mock으로만 JPA mapping, PostgreSQL constraint, Redis Stream consumer 동작을 검증했다고 주장하지 않는다.

## 점검 기준

- 신규 business rule에 unit test가 있는지 확인한다.
- 외부 인프라 연동에 integration test가 있는지 확인한다.
- 실패/예외/권한 경로 테스트가 포함되어 있는지 확인한다.
- 테스트가 병렬 실행과 반복 실행에 안전한지 확인한다.

## 근거 문서

- Spring Framework Testing: https://docs.spring.io/spring-framework/reference/testing.html
- Spring Boot Testing: https://docs.spring.io/spring-boot/reference/testing/
- Spring Boot Testcontainers: https://docs.spring.io/spring-boot/reference/testing/testcontainers.html
- Spring Framework MockMvc: https://docs.spring.io/spring-framework/reference/testing/mockmvc.html
