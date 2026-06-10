# WebSocket and STOMP Rules

## Scope

- 이 규칙은 Spring WebSocket, STOMP endpoint, message mapping, subscription, payload, 인증/인가, 오류 처리를 정의한다.
- WebSocket은 실시간 양방향 통신에 사용하고, 일반 CRUD 조회/수정은 REST API를 우선한다.

## Endpoint and Destination

- WebSocket handshake endpoint는 `/ws`를 기본으로 한다.
- client publish prefix는 `/app`을 사용한다.
- broadcast subscription prefix는 `/topic`을 사용한다.
- user-specific subscription prefix는 `/user/queue`를 사용한다.
- 채팅방 메시지 broadcast destination은 `/topic/chat-rooms/{roomId}/messages`를 사용한다.
- 사용자별 ack, error, notification은 `/user/queue/...`를 사용한다.

```text
CONNECT /ws
SEND      /app/chat-rooms/{roomId}/messages
SUBSCRIBE /topic/chat-rooms/{roomId}/messages
SUBSCRIBE /user/queue/chat/errors
```

## Payload

- REST DTO와 STOMP DTO는 분리한다.
- 모든 client request payload는 `requestId`, `type`, `payload`, `sentAt`을 포함한다.
- 모든 server event payload는 `eventId`, `type`, `payload`, `occurredAt`, `traceId`를 포함한다.
- `type`은 domain event 이름을 upper snake case로 작성한다. 예: `CHAT_MESSAGE_CREATED`
- payload field는 camelCase를 사용한다.

## Controller and Service

- `@MessageMapping` method는 인증 principal, destination variable, payload validation, application service 호출만 담당한다.
- 메시지 저장, 권한 검증, Redis Stream publish는 application service에서 처리한다.
- `SimpMessagingTemplate` 직접 사용은 outbound adapter 또는 application service 경계로 제한한다.

## Authentication and Authorization

- CONNECT 단계에서 인증 정보를 검증한다.
- room subscription과 message send는 room membership 또는 권한 정책을 검사한다.
- destination path의 `roomId`, `userId`만 신뢰하지 않는다.
- user destination은 authenticated principal 기준으로 라우팅한다.

## Ordering and Delivery

- 같은 room의 메시지는 server-assigned sequence 또는 persisted timestamp를 포함한다.
- client는 중복 eventId를 무시할 수 있어야 한다.
- server는 STOMP 전송 성공을 DB commit 성공과 동일하게 간주하지 않는다.
- 장애 후 재동기화를 위해 REST 기반 message history 조회를 제공한다.

## Error Handling

- client 요청 처리 실패는 `/user/queue/chat/errors`로 전송한다.
- error payload는 `requestId`, `code`, `message`, `traceId`, `occurredAt`을 포함한다.
- 내부 예외, stack trace, SQL, Redis command detail은 client에 노출하지 않는다.

## 금지

- WebSocket handler에서 Entity를 직접 전송하지 않는다.
- 인증되지 않은 사용자가 임의 destination으로 publish/subscribe하도록 허용하지 않는다.
- broadcast channel에 개인 정보 또는 개인 오류를 전송하지 않는다.
- STOMP destination에 command 동사를 남발하지 않는다. command가 필요하면 `/app/.../<command-resource>`로 모델링한다.

## 예외

- 운영 모니터링용 내부 WebSocket endpoint는 별도 prefix를 사용할 수 있다.
- broker relay를 사용하는 경우 broker별 destination 제한은 별도 설정 문서에 따른다.

## 점검 기준

- 모든 `@MessageMapping` 요청에 validation과 authorization이 있는지 확인한다.
- server event가 idempotency 판단에 필요한 `eventId`를 포함하는지 확인한다.
- client 재접속 시 누락 메시지를 복구할 REST 조회 경로가 있는지 확인한다.
- 개인 응답이 `/topic`이 아니라 user destination으로 전송되는지 확인한다.

## 근거 문서

- Spring Framework STOMP: https://docs.spring.io/spring-framework/reference/web/websocket/stomp.html
- Spring Framework STOMP Authentication: https://docs.spring.io/spring-framework/reference/web/websocket/stomp/authentication.html
- Spring Framework STOMP User Destinations: https://docs.spring.io/spring-framework/reference/web/websocket/stomp/user-destination.html
- Spring Boot WebSockets: https://docs.spring.io/spring-boot/reference/messaging/websockets.html
