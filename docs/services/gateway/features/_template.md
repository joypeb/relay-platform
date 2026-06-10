# Feature Name

## Purpose

이 기능이 해결하는 문제와 사용자 또는 시스템 관점의 역할을 설명한다.

## Scope

- 포함하는 동작:
- 포함하지 않는 동작:

## End-To-End Flow

1. 요청 또는 이벤트가 어디서 들어오는지 설명한다.
2. 어떤 컴포넌트를 거치는지 설명한다.
3. 어떤 저장소나 외부 시스템을 사용하는지 설명한다.
4. 어떤 응답 또는 이벤트가 나가는지 설명한다.

## Core Logic

- 주요 검증 규칙:
- 주요 분기:
- 트랜잭션 경계:
- 중복 방지 또는 idempotency:
- 재시도 또는 보상 처리:

## Related Code

- `gateway/src/main/java/...`
- `gateway/src/main/resources/application.yaml`

## Contracts

### REST API

- Method:
- Path:
- Request:
- Response:
- Status codes:

### WebSocket / STOMP

- Endpoint:
- Subscribe destination:
- Send destination:
- Payload:

### Redis

- Key:
- TTL:
- Stream:
- Consumer group:
- Event schema:

### Database

- Table:
- Columns:
- Indexes:
- Constraints:

## Failure Handling

- 실패 조건:
- 예외 타입:
- 클라이언트 응답:
- 로그와 모니터링 포인트:

## Tests

- Unit tests:
- Integration tests:
- Manual verification:

## Design Decisions

- 선택한 방식:
- 대안:
- trade-off:
- 관련 ADR:
