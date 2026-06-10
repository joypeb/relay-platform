# Project Common Rules

## Scope

- 이 규칙은 Java 21, Spring Boot 4, Spring Web MVC, JPA, PostgreSQL, WebSocket, STOMP, Redis, Redis Streams, MSA 기반 서비스를 대상으로 한다.
- 프로젝트의 모든 서비스는 독립 배포 가능한 Spring Boot 애플리케이션이어야 한다.
- 규칙 간 충돌이 있으면 더 구체적인 도메인 규칙을 우선한다.

## 반드시

- Java 21 기능은 가독성과 안정성이 명확할 때만 사용한다. `record`는 불변 DTO, 값 객체, 이벤트 payload에 우선 사용한다.
- Spring Boot가 제공하는 auto-configuration, configuration properties, actuator, test slice, Testcontainers 지원을 우선 사용한다.
- 애플리케이션 설정은 코드에 하드코딩하지 않고 `application.yml`, 환경 변수, config server 중 하나로 외부화한다.
- `@SpringBootApplication` 클래스는 최상위 base package에 둔다.
- bean 의존성 주입은 생성자 주입을 기본으로 한다.
- API, WebSocket, Redis Stream, DB schema, event payload는 버전 변경 시 하위 호환성을 검토한다.
- 모든 서비스는 health check, structured logging, metrics, tracing 전파 기준을 가져야 한다.

## 금지

- Controller, WebSocket handler, Redis listener, scheduler에서 비즈니스 규칙을 직접 구현하지 않는다.
- Entity를 REST 응답, WebSocket payload, Redis Stream payload로 직접 노출하지 않는다.
- 운영 코드에서 테스트 편의를 위한 public setter, package-private 우회 메서드, static mutable state를 만들지 않는다.
- `System.out`, `printStackTrace`, 임시 로그 문자열로 운영 로그를 남기지 않는다.
- `@Autowired` field injection을 사용하지 않는다.
- 순환 패키지 의존성, 공통 모듈에 대한 도메인 역의존, 서비스 간 DB 공유를 만들지 않는다.

## 패키지 구조

- 서비스 내부는 기능 중심 패키지를 기본으로 한다.

```text
com.example.<service>
  <domain>
    api
    application
    domain
    infrastructure
    dto
    event
    config
```

- `api`는 REST/WebSocket adapter만 포함한다.
- `application`은 유스케이스 orchestration과 transaction boundary를 포함한다.
- `domain`은 entity, value object, domain service, domain event를 포함한다.
- `infrastructure`는 JPA, Redis, 외부 API client, message adapter를 포함한다.
- `dto`는 transport별 DTO를 분리한다. REST DTO와 STOMP DTO는 공유하지 않는다.

## 예외

- 단일 gateway 또는 edge service처럼 도메인 모델이 없는 서비스는 `api`, `config`, `filter`, `client` 구조를 사용할 수 있다.
- 아주 작은 설정 전용 모듈은 기능 중심 패키지를 생략할 수 있다.

## 점검 기준

- `@SpringBootApplication`이 base package scan을 안정적으로 포함하는 위치에 있는지 확인한다.
- 주요 입력/출력 모델이 Entity가 아닌 DTO인지 확인한다.
- 운영 로그, metrics, tracing이 실패 경로에도 남는지 확인한다.
- 테스트 코드 외부에서 field injection, static mutable state, hard-coded secret이 없는지 확인한다.

## 근거 문서

- Spring Boot Reference: https://docs.spring.io/spring-boot/reference/
- Spring Boot Structuring Your Code: https://docs.spring.io/spring-boot/reference/using/structuring-your-code.html
- Spring Boot Externalized Configuration: https://docs.spring.io/spring-boot/reference/features/external-config.html
- Spring Boot Observability: https://docs.spring.io/spring-boot/reference/actuator/observability.html
