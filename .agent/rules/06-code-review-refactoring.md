# Code Review and Refactoring Rules

## Scope

- 이 규칙은 코드 점검, 리팩토링, 이름, 중복 제거, logging, tracing, metrics, abstraction, design pattern 선택 기준을 정의한다.
- 리뷰는 취향이 아니라 아래 점검 기준을 근거로 수행한다.

## Naming

- class 이름은 책임을 드러내는 명사 또는 명사구를 사용한다.
- method 이름은 행위를 드러내는 동사 또는 동사구를 사용한다.
- boolean 이름은 `is`, `has`, `can`, `should`, `requires` 중 하나로 시작한다.
- `data`, `info`, `temp`, `result`, `manager`, `processor`, `handler` 단독 이름은 금지한다.
- domain 용어와 transport 용어를 섞지 않는다. 예: `StompMessageEntity` 금지.

## Readability

- 하나의 method는 하나의 의도를 가져야 한다.
- 3단계 이상 중첩 조건문은 guard clause, method extraction, policy object 중 하나로 단순화한다.
- method parameter가 4개 이상이면 command object 또는 value object를 검토한다.
- 주석은 "무엇"이 아니라 "왜"를 설명할 때만 작성한다.
- public method는 호출자가 알 필요 없는 구현 세부사항을 이름에 노출하지 않는다.

## Duplication

- 같은 business rule이 두 곳 이상 있으면 domain method, policy, validator, shared application service 중 하나로 통합한다.
- DTO 변환 중복은 mapper로 통합한다.
- test fixture 중복은 test data builder 또는 fixture factory로 통합한다.
- 단순히 2줄이 같다는 이유만으로 추상화하지 않는다. 변경 이유가 같을 때만 통합한다.

## Logging

- 외부 API 호출, Redis command 실패, Redis Stream consume 실패, DB transaction 실패, authentication/authorization 실패는 로그를 남긴다.
- 정상적인 고빈도 메시지 처리 로그는 debug 또는 sampled info로 제한한다.
- log에는 `traceId`, `spanId`, resource id, event id, command id 중 추적에 필요한 값을 포함한다.
- password, token, authorization header, session id, raw personal data는 로그에 남기지 않는다.

## Metrics and Tracing

- 주요 use case latency, error count, queue lag, stream pending count, WebSocket connection count는 metrics로 관찰 가능해야 한다.
- service boundary를 넘는 REST call, message publish/consume, scheduled job에는 tracing context를 전파한다.
- metric 이름은 domain 의미와 단위를 포함한다.

## Interface and Abstraction

- interface는 구현체가 2개 이상이거나 외부 의존성을 격리해야 할 때 만든다.
- 단순 관습 때문에 `XService`와 `XServiceImpl`을 1:1로 만들지 않는다.
- abstraction은 호출자 관점의 안정적인 계약을 표현해야 한다.
- generic utility는 세 번째 사용처가 생기기 전까지 만들지 않는다.

## Model and Design Pattern

- domain model은 상태와 invariant를 함께 보유해야 한다.
- 값 객체는 불변으로 만든다.
- strategy pattern은 정책이 런타임/설정/도메인 조건에 따라 바뀔 때 사용한다.
- factory는 생성 규칙이 복잡하거나 invariant를 강제할 때 사용한다.
- template method보다 composition과 strategy를 우선 검토한다.
- event-driven pattern은 transaction, idempotency, ordering, retry 기준이 함께 있을 때만 도입한다.

## Error-Prone Code

- null 가능성이 있는 값은 `Optional`, validation, explicit exception 중 하나로 경계를 명확히 한다.
- 시간은 `Instant`를 저장 기준으로 사용하고, client 표시 시점에 timezone을 적용한다.
- money, count, sequence 같은 값은 primitive obsession을 피하고 value object를 검토한다.
- string literal로 domain state를 표현하지 않는다. enum 또는 type-safe value를 사용한다.

## 금지

- 실패를 무시하는 빈 catch block을 만들지 않는다.
- broad catch 후 재던짐 없이 로그만 남기지 않는다.
- business rule을 controller annotation 또는 if문 조각으로 흩뜨리지 않는다.
- 테스트를 깨지 않기 위해 운영 코드를 약하게 만들지 않는다.

## 점검 기준

- 이름만 보고 method 역할과 반환 의미를 알 수 있는지 확인한다.
- 같은 규칙이 controller, service, listener에 중복되어 있지 않은지 확인한다.
- 실패 로그와 metrics가 원인 분석에 충분한지 확인한다.
- abstraction이 실제 변경 가능성을 줄이는지 확인한다.
- design pattern이 코드량만 늘리고 있지 않은지 확인한다.

## 근거 문서

- Spring Boot Logging: https://docs.spring.io/spring-boot/reference/features/logging.html
- Spring Boot Observability: https://docs.spring.io/spring-boot/reference/actuator/observability.html
- Spring Boot Metrics: https://docs.spring.io/spring-boot/reference/actuator/metrics.html
- Spring Boot Tracing: https://docs.spring.io/spring-boot/reference/actuator/tracing.html
