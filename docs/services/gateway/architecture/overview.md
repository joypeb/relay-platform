# Gateway Architecture Overview

이 문서는 `gateway` 서비스의 내부 구조와 요청 처리 흐름을 설명한다.

## Responsibility

- 서비스 책임:
  - 외부 클라이언트의 REST, WebSocket/STOMP 진입점을 제공한다.
  - 테스트용 로컬 로그인 세션을 생성하고, WebSocket handshake와 STOMP CONNECT에서 인증된 principal로 변환한다.
  - 인증된 REST chat-room 요청을 chat-service로 전달하고 세션 사용자를 `X-User-Id` header로 전파한다.
  - STOMP destination prefix 정책을 적용해 인증되지 않은 publish/subscribe를 차단한다.
  - STOMP connect/disconnect 이벤트를 수집해 현재 활성 연결을 추적한다.
- 외부에 제공하는 계약:
  - REST: `POST /api/v1/sessions`, `GET /api/v1/sessions/current`, `DELETE /api/v1/sessions/current`
  - REST routing: `POST|GET|PATCH|DELETE /api/v1/chat-rooms...`
  - WebSocket handshake: `/ws`
  - STOMP send prefix: `/app`
  - STOMP broadcast subscription prefix: `/topic`
  - STOMP user subscription prefix: `/user/queue`
- 라우팅 대상 서비스:
  - chat-service: chat-room REST API 원천 서비스
- 의존하는 외부 시스템:
  - chat-service HTTP endpoint. base URL은 `gateway.chat-service.base-url`로 설정한다.
  - 테스트용 세션은 gateway 로컬 servlet session을 사용한다.

## Package Or Module Structure

- `gateway/src/main/java/com/joypeb/gateway`: gateway 애플리케이션 코드.
  - `api.rest`: 로컬 session REST API와 REST 오류 응답 adapter.
  - `api.stomp`: STOMP `@MessageMapping` adapter.
  - `config`: Spring Cloud Gateway Server MVC route, 설정 properties, clock 설정.
  - `dto.rest`: REST 요청/응답 DTO.
  - `dto.stomp`: STOMP request/event DTO.
  - `session`: servlet session 속성, principal, 인증 예외.
  - `websocket.config`: WebSocket/STOMP broker 설정과 configuration properties.
  - `websocket.handshake`: WebSocket handshake 인증과 principal 변환.
  - `websocket.security`: STOMP frame 인증/인가 정책.
  - `websocket.connection`: STOMP 연결 상태 추적과 connect/disconnect 이벤트 처리.
  - `websocket.error`: STOMP ERROR frame 변환.
- `gateway/src/main/resources`: gateway 설정.
- `gateway/src/test`: gateway 테스트.

## Main Flows

### Local Session Login

1. 클라이언트 요청이 gateway로 들어온다.
2. `POST /api/v1/sessions`가 `userId`를 검증한다.
3. gateway가 servlet session을 생성하고 `AUTHENTICATED_USER_ID`를 저장한다.
4. 응답 body는 공통 wrapper로 `userId`, `sessionId`, `traceId`, `timestamp`를 반환한다.

### WebSocket And STOMP

1. 클라이언트가 로그인 세션 쿠키를 포함해 `/ws`로 WebSocket handshake를 요청한다.
2. `GatewayHandshakeInterceptor`가 세션의 `AUTHENTICATED_USER_ID` 존재 여부를 확인한다.
3. `GatewayHandshakeHandler`가 세션 사용자를 `Principal`로 변환한다.
4. STOMP `CONNECT`, `SUBSCRIBE`, `SEND`는 `StompAuthenticationChannelInterceptor`에서 인증과 destination prefix를 검사한다.
5. `SessionConnectEvent`, `SessionDisconnectEvent`는 `StompConnectionRegistry`에 반영된다.
6. 현재 gateway는 연결 검증용 `/app/gateway/acks`만 처리하며, 채팅 도메인 메시지 저장과 Redis publish는 담당하지 않는다.

### Chat Room REST Routing

1. 클라이언트가 세션 cookie와 함께 `/api/v1/chat-rooms...`를 호출한다.
2. Spring Cloud Gateway Server MVC route가 `/api/v1/chat-rooms`와 `/api/v1/chat-rooms/**`를 매칭한다.
3. `ChatServiceGatewayRouteConfig`의 filter가 session에서 `AUTHENTICATED_USER_ID`를 확인한다.
4. 인증되지 않은 요청은 `UnauthenticatedException`으로 중단되고 gateway `ProblemDetail` 응답으로 변환된다.
5. 인증된 요청은 원래 path, query string, request body를 유지해 chat-service로 전달된다.
6. gateway는 session cookie를 chat-service에 전달하지 않고, 클라이언트가 보낸 `X-User-Id`를 session 사용자 ID로 덮어쓴다.
7. chat-service의 status, response header, body를 gateway 응답으로 반환한다.

## Cross-Cutting Concerns

- 인증/인가:
  - 테스트용 로그인은 `userId`만 받는다.
  - chat-room REST routing은 gateway session을 service-to-service `X-User-Id` header로 변환한다.
  - WebSocket handshake와 STOMP CONNECT/SUBSCRIBE/SEND는 인증된 session principal이 필요하다.
  - SUBSCRIBE는 `/topic/**`, `/user/queue/**`만 허용한다.
  - SEND는 `/app/**`만 허용한다.
- 로깅:
  - STOMP connect/disconnect 이벤트를 structured key-value 형태로 남긴다.
- tracing:
  - REST 응답은 `X-Trace-Id` 요청 헤더가 있으면 이를 사용하고, 없으면 UUID를 생성한다.
  - chat-room REST routing은 Spring Cloud Gateway Server MVC route를 통해 tracing header가 chat-service로 전달될 수 있게 한다.
  - STOMP ACK 이벤트는 `traceId` 필드를 포함한다.
- metrics:
  - actuator metrics endpoint를 노출한다.
  - STOMP 활성 연결 수는 `StompConnectionRegistry`에서 추적한다.
- health check:
  - actuator health endpoint를 노출한다.
- 오류 응답:
  - REST validation/authentication 실패는 `ProblemDetail` 호환 구조로 응답한다.
  - STOMP 인증/권한 실패는 STOMP ERROR frame으로 응답한다.
