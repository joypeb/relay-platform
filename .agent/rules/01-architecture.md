# Architecture Rules

## Scope

- 이 규칙은 MSA 서비스 경계, 서비스 내부 계층, 의존 방향, 장애 격리 기준을 정의한다.
- Spring Boot 4와 Spring Cloud 계열 도구를 사용할 수 있으나, 특정 Spring Cloud 컴포넌트 사용은 아키텍처 결정 기록으로 남긴다.

## 반드시

- 각 microservice는 독립 배포, 독립 설정, 독립 장애 격리가 가능한 단위여야 한다.
- 서비스 경계는 기술 계층이 아니라 비즈니스 capability와 데이터 소유권 기준으로 나눈다.
- 한 서비스는 자신이 소유한 PostgreSQL schema/table만 직접 읽고 쓴다.
- 다른 서비스의 데이터는 REST API, event, message, read model 중 하나로 접근한다.
- service-to-service 호출은 timeout, retry, circuit breaker 또는 fallback 정책을 명시한다.
- gateway는 routing, authentication delegation, rate limiting, cross-cutting concern만 담당한다.
- distributed tracing header는 REST, WebSocket handshake, message processing 경계에서 보존한다.

## 서비스 내부 아키텍처

- `api` 계층은 request validation, authentication principal 추출, response mapping만 담당한다.
- `application` 계층은 use case 단위 메서드를 제공하고 transaction boundary를 가진다.
- `domain` 계층은 business invariant를 보호한다.
- `infrastructure` 계층은 JPA, Redis, 외부 HTTP client, broker client 구현을 담당한다.
- 외부 시스템 adapter는 interface 뒤에 숨기고 application 계층은 구체 client에 직접 의존하지 않는다.

## 의존 방향

- 허용: `api -> application -> domain`
- 허용: `application -> infrastructure interface`
- 허용: `infrastructure implementation -> domain/application port`
- 금지: `domain -> api`
- 금지: `domain -> infrastructure`
- 금지: `infrastructure -> api`

## MSA 데이터 규칙

- cross-service transaction을 기본 설계로 사용하지 않는다.
- 서비스 간 정합성은 eventual consistency, outbox, compensating action 중 하나로 설계한다.
- 같은 aggregate를 여러 서비스가 동시에 소유하지 않는다.
- read model은 원천 데이터가 아니며 재생성 가능해야 한다.

## 금지

- 서비스 A가 서비스 B의 DB table을 직접 조회하거나 수정하지 않는다.
- gateway에 도메인별 비즈니스 규칙을 넣지 않는다.
- 공통 모듈에 도메인 모델을 몰아넣어 서비스 경계를 무력화하지 않는다.
- synchronous chaining이 3개 이상의 서비스를 연쇄 호출하게 만들지 않는다.

## 예외

- observability, authentication, shared error contract 같은 cross-cutting contract는 공통 모듈로 둘 수 있다.
- 데이터 분석, 감사, 검색용 read-only projection은 원천 서비스의 event 또는 CDC 기반으로 별도 저장소에 구성할 수 있다.

## 점검 기준

- 서비스별 owned data와 외부 의존성이 문서화되어 있는지 확인한다.
- 신규 API가 다른 서비스 DB 구조를 전제로 하지 않는지 확인한다.
- 실패 전파 경로에 timeout, retry limit, fallback, alerting이 있는지 확인한다.
- 이벤트 기반 흐름은 중복 수신, 순서 뒤바뀜, 재처리를 견딜 수 있는지 확인한다.

## 근거 문서

- Spring Microservices: https://spring.io/microservices/
- Spring Cloud: https://spring.io/projects/spring-cloud/
- Spring Cloud Commons: https://docs.spring.io/spring-cloud-commons/reference/spring-cloud-commons/common-abstractions.html
- Spring Boot Observability: https://docs.spring.io/spring-boot/reference/actuator/observability.html
