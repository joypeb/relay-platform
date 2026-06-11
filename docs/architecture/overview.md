# Architecture Overview

이 문서는 프로젝트의 전체 구조와 서비스 경계를 설명한다.

현재 프로젝트는 `gateway`, `chat-service` 서비스를 포함한다. 서비스별 내부 구조와 기능은 `docs/services/<service-name>/` 아래에 작성하고, 이 문서는 여러 서비스에 걸친 경계와 통신 흐름만 기록한다.

## Modules

### gateway

- 서비스 문서: `docs/services/gateway/README.md`
- 역할: edge gateway
- 주요 책임: 로컬 세션 인증, WebSocket/STOMP 연결 경계
- 외부에 노출하는 계약: `/api/v1/sessions`, `/ws`
- 의존하는 외부 시스템: Redis, PostgreSQL

### chat-service

- 서비스 문서: `docs/services/chat-service/README.md`
- 역할: 채팅 도메인 원천 데이터 소유 서비스
- 주요 책임: 채팅방 생성, 목록 조회, 상세 조회, 수정, 삭제와 채팅방 멤버십 저장
- 외부에 노출하는 계약: `/api/v1/chat-rooms`
- 의존하는 외부 시스템: PostgreSQL

## Cross-Cutting Concerns

- 인증/인가: gateway가 인증 경계를 담당하고, `chat-service`는 `X-User-Id` header를 actor 식별자로 사용한다.
- 로깅:
- tracing:
- metrics: actuator metrics endpoint를 사용한다.
- health check: actuator health endpoint를 사용한다.
- 오류 응답: REST API는 `ProblemDetail` 호환 오류 응답을 사용한다.

## Realtime Chat Message Flow

채팅 메시지는 `chat-service`가 PostgreSQL에 저장하고 방별 sequence를 부여한다. 저장 commit 이후 `stream:chat:message-created` Redis Stream에 `CHAT_MESSAGE_CREATED` 이벤트를 발행한다.

gateway는 instance별 consumer group을 사용한다. 예를 들어 `gateway-1`은 `gateway-broadcast-gateway-1`, `gateway-2`는 `gateway-broadcast-gateway-2` group으로 같은 stream을 읽는다. Redis Streams의 단일 consumer group은 메시지를 consumer 중 하나에게만 분배하므로, 다중 gateway broadcast에는 공유 group을 사용하지 않는다.

각 gateway는 수신한 stream event를 자기 instance에 연결된 WebSocket client에게 `/topic/chat-rooms/{roomId}/messages`로 broadcast한다. gateway가 중단된 동안 놓친 메시지는 STOMP로 복구하지 않고 REST history API로 복구한다.

## Related Documents

- `docs/services/`
- `docs/adr/`
