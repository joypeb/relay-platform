# Use Spring Cloud Gateway Server MVC

## Status

Accepted

## Context

chat-service에 REST API가 추가될 때마다 gateway에 대응 controller를 추가하면 gateway가 서비스별 API 표면을 계속 복제하게 된다. gateway 책임은 routing, 인증 위임, cross-cutting concern에 머물러야 하므로 route 설정과 filter 중심 구조가 필요하다.

현재 gateway는 Spring Web MVC, servlet session, WebSocket/STOMP 설정을 사용한다. WebFlux 기반 Gateway로 전환하면 실행 모델, 세션 처리, STOMP 인증 흐름까지 함께 재설계해야 한다.

## Decision

gateway의 chat-room REST 연결은 Spring Cloud Gateway Server MVC를 사용한다.

- dependency: `org.springframework.cloud:spring-cloud-starter-gateway-server-webmvc`
- route: `/api/v1/chat-rooms`, `/api/v1/chat-rooms/**` -> `gateway.chat-service.base-url`
- handler: Spring Cloud Gateway MVC `HandlerFunctions.http()`
- auth propagation: route filter가 servlet session의 `AUTHENTICATED_USER_ID`를 읽고 downstream `X-User-Id` header로 전파한다.
- edge-local cookie와 클라이언트가 보낸 `X-User-Id`는 chat-service로 신뢰 전파하지 않는다.

## Consequences

### Positive

- chat-service 하위 API가 늘어나도 동일 path prefix 아래에서는 gateway controller를 추가하지 않아도 된다.
- gateway가 chat-room 도메인 request/response DTO를 소유하지 않는다.
- servlet session과 기존 WebSocket/STOMP 구성을 유지하면서 gateway routing 기능을 사용할 수 있다.

### Negative

- Spring Cloud Gateway Server MVC dependency와 route/filter API 학습 비용이 추가된다.
- route별 timeout, retry, circuit breaker 정책은 별도 설정 또는 후속 ADR로 명시해야 한다.
- WebFlux Gateway보다 reactive streaming 또는 high-concurrency proxy 특화 이점은 작다.

### Neutral

- gateway는 여전히 chat-service availability에 동기적으로 의존한다.
- 운영 인증 모델이 도입되면 session 기반 `X-User-Id` 전파는 token relay 또는 service identity 기반 전파로 교체될 수 있다.

## Alternatives Considered

- 수동 Spring MVC forwarding controller 유지:
  - 단순하지만 API prefix가 늘어날수록 controller와 HTTP forwarding 코드가 반복된다.
- 자체 generic forwarding controller 작성:
  - dependency를 줄일 수 있지만 routing, filter, header 처리, timeout 같은 gateway concern을 직접 유지보수해야 한다.
- Spring Cloud Gateway WebFlux 사용:
  - gateway 전용 실행 모델로 적합하지만 현재 servlet session과 STOMP 구성을 함께 바꾸는 마이그레이션 비용이 크다.

## Related Code Or Docs

- `gateway/build.gradle`
- `gateway/src/main/java/com/joypeb/gateway/config/ChatServiceGatewayRouteConfig.java`
- `gateway/src/main/java/com/joypeb/gateway/config/ChatServiceProperties.java`
- `gateway/src/test/java/com/joypeb/gateway/api/rest/ChatRoomGatewayRouteTest.java`
- `docs/services/gateway/features/local-session-and-stomp-gateway.md`
