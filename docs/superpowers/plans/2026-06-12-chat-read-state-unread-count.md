# Chat Read State And Unread Count Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 채팅방별 마지막 메시지 sequence와 사용자별 마지막 읽은 sequence를 기반으로 읽음 처리와 안읽은 메시지 수를 제공한다.

**Architecture:** PostgreSQL은 메시지와 최종 읽음 상태의 영구 원천 저장소로 유지하고, Redis는 최신 sequence read model과 30초 단위 DB write-behind buffer로 사용한다. 모든 sequence 갱신은 단조 증가 규칙을 적용해 오래된 메시지 이벤트나 읽음 ACK가 최신 값을 덮어쓰지 못하게 한다.

**Tech Stack:** Java 21, Spring Boot 4, Spring Web MVC, Spring WebSocket/STOMP, Spring Data JPA, PostgreSQL, Spring Data Redis, Redis Stream, Redis String/Hash/Set.

---

## Scope

이 계획은 다음 기능을 포함한다.

- 채팅방의 마지막 메시지 sequence를 Redis에 저장하고, Redis miss 시 DB에서 복구한다.
- 사용자별 마지막 읽은 sequence를 Redis에 저장하고, Redis miss 시 DB에서 복구한다.
- 메시지 전송자는 전송 성공 후 자신의 `lastReadSequence`가 전송 메시지 sequence까지 올라간다.
- 프론트엔드가 WebSocket으로 읽음 ACK를 보내면 Redis에 즉시 반영한다.
- 사용자별 읽음 상태는 30초 단위로 모아서 PostgreSQL에 batch upsert 한다.
- 채팅방 목록 조회 응답에 unread count를 포함한다.

이 계획은 다음 기능을 포함하지 않는다.

- 메시지별 읽은 사람 목록 표시
- 방 인원별 read receipt broadcast
- 푸시 알림
- Redis 장애 시 30초 미flush 읽음 상태의 완전 무손실 보장

## Existing Context

- 메시지 저장은 `chat-service`가 담당한다.
- gateway는 STOMP ingress와 Redis Stream 기반 broadcast를 담당한다.
- 메시지 생성 이벤트는 `stream:chat:message-created`에 발행된다.
- 메시지 sequence는 `chat_message_sequences`에서 방 단위로 발급된다.
- 클라이언트는 `/topic/chat-rooms/{roomId}/messages`를 구독해 메시지 생성 이벤트를 받는다.

## Data Ownership

### PostgreSQL

PostgreSQL은 다음 데이터의 영구 원천 저장소다.

- 채팅 메시지 본문과 sequence
- 방별 sequence 발급 상태
- 사용자별 마지막 읽은 sequence의 최종 반영 상태

### Redis

Redis는 다음 데이터의 최신 read model이다.

- 방별 마지막 메시지 sequence
- 사용자별 방 읽음 sequence
- DB batch flush 대상 dirty entry

Redis 데이터는 DB에서 재구성 가능해야 한다. 단, 읽음 ACK가 Redis에만 반영되고 아직 DB에 flush되지 않은 30초 이내 상태는 Redis 장애 또는 flush 전 재시작 시 유실될 수 있다.

## Redis Key Design

### Room Last Sequence

| Item | Value |
| --- | --- |
| Key | `chat:room:{roomId}:last-sequence` |
| Type | String |
| Value | 해당 채팅방에 저장 완료된 마지막 메시지 sequence |
| TTL | 없음 |
| Writer | `chat-service` 메시지 저장 commit 이후 |
| Reader | `chat-service` 채팅방 목록 조회, 읽음 ACK sequence 검증 |
| Update rule | `max(existingSequence, newMessageSequence)` |
| Miss handling | DB의 `chat_message_sequences.next_sequence - 1` 또는 메시지가 없으면 `0`을 조회한 뒤 Redis에 저장 |

### User Room Read Sequences

| Item | Value |
| --- | --- |
| Key | `chat:user:{userId}:room-read-sequences` |
| Type | Hash |
| Field | `{roomId}` |
| Value | 해당 사용자가 해당 방에서 마지막으로 읽은 메시지 sequence |
| TTL | 없음 |
| Writer | WebSocket 읽음 ACK 처리, 메시지 전송자 자동 읽음 처리, DB miss 복구 |
| Reader | 채팅방 목록 조회, unread count 계산 |
| Update rule | `max(existingReadSequence, incomingReadSequence)` |
| Miss handling | DB의 `chat_room_read_states.last_read_sequence` 또는 row가 없으면 `0`을 조회한 뒤 Redis hash에 저장 |

### Dirty Read State Set

| Item | Value |
| --- | --- |
| Key | `chat:read-state:dirty` |
| Type | Set |
| Member | `{roomId}:{userId}` |
| TTL | 없음 |
| Writer | 읽음 상태가 Redis에서 증가한 경우 |
| Reader | 30초 batch flush scheduler |
| Remove rule | 해당 member의 최신 Redis read sequence를 DB에 성공적으로 upsert한 뒤 제거 |

### Dirty Entry Encoding

`chat:read-state:dirty`의 member는 `roomId:userId` 형식을 사용한다.

- `roomId`는 UUID 문자열이다.
- `userId`는 현재 프로젝트의 authenticated principal name 문자열이다.
- 구분자는 `:`를 사용한다.
- 구현 시 `userId`에 `:`가 들어갈 수 있는 인증 체계로 바뀌면 member encoding을 JSON 또는 별도 hash 구조로 바꿔야 한다.

## Redis Atomicity Rules

Redis 갱신은 원자적으로 처리해야 한다.

### Room Last Sequence Update

방 마지막 sequence 갱신은 다음을 하나의 원자 연산으로 처리한다.

1. `chat:room:{roomId}:last-sequence`의 현재 값을 읽는다.
2. 현재 값이 없거나 새 sequence보다 작으면 새 sequence를 저장한다.
3. 현재 값이 새 sequence 이상이면 아무것도 변경하지 않는다.

### User Read Sequence Update

사용자 읽음 sequence 갱신은 다음을 하나의 원자 연산으로 처리한다.

1. `chat:user:{userId}:room-read-sequences` hash의 `{roomId}` field를 읽는다.
2. 현재 값이 없거나 incoming sequence보다 작으면 incoming sequence를 저장한다.
3. 실제 값이 증가한 경우에만 `chat:read-state:dirty`에 `{roomId}:{userId}`를 추가한다.
4. 현재 값이 incoming sequence 이상이면 아무것도 변경하지 않는다.

이 처리는 Lua script 또는 Redis transaction으로 구현한다. 단순 `HGET` 후 `HSET`을 별도 command로 나누면 늦게 도착한 오래된 ACK가 최신 read sequence를 덮어쓸 수 있다.

## PostgreSQL Design

### Read State Table

새 테이블은 사용자별 방 읽음 상태를 저장한다.

| Column | Meaning |
| --- | --- |
| `room_id` | 채팅방 ID |
| `member_id` | 사용자 ID |
| `last_read_sequence` | 사용자가 마지막으로 읽은 메시지 sequence |
| `created_at` | row 생성 시각 |
| `updated_at` | 마지막 반영 시각 |

제약 조건은 다음을 둔다.

- primary key: `(room_id, member_id)`
- `last_read_sequence >= 0`
- 자주 조회되는 `member_id`에는 index를 둔다.

### DB Update Rule

batch flush는 upsert를 사용한다.

- row가 없으면 새 row를 생성한다.
- row가 있으면 `last_read_sequence`를 `max(existing, incoming)`으로 갱신한다.
- incoming sequence가 기존 값보다 작거나 같으면 값은 유지하고, 필요하면 `updated_at` 갱신도 생략한다.

DB 역시 단조 증가 규칙을 지켜야 한다. Redis flush 작업이 중복 실행되거나 오래된 dirty entry를 처리해도 read state가 뒤로 가지 않아야 한다.

## REST API Design

### Chat Room List

기존 API를 확장한다.

| Item | Value |
| --- | --- |
| Method | `GET` |
| Path | `/api/v1/chat-rooms` |
| Query | 기존 `scope`, pagination query 유지 |
| Header | `X-User-Id` |
| Response addition | 각 방 summary에 `lastMessageSequence`, `lastReadSequence`, `unreadCount` 추가 |

응답 의미는 다음과 같다.

| Field | Meaning |
| --- | --- |
| `lastMessageSequence` | 해당 방의 마지막 메시지 sequence |
| `lastReadSequence` | 요청 사용자가 해당 방에서 마지막으로 읽은 sequence |
| `unreadCount` | `max(0, lastMessageSequence - lastReadSequence)` |

Redis miss 처리 흐름은 다음과 같다.

1. 목록에 포함된 roomId를 수집한다.
2. 각 방의 `chat:room:{roomId}:last-sequence`를 Redis에서 조회한다.
3. 없는 값은 DB에서 조회한 뒤 Redis에 저장한다.
4. 요청 사용자의 `chat:user:{userId}:room-read-sequences` hash에서 각 roomId field를 조회한다.
5. 없는 값은 DB에서 조회한 뒤 Redis에 저장한다.
6. Redis 값과 DB 복구 값을 합쳐 unread count를 계산한다.

### Internal Read Receipt API

gateway가 STOMP 읽음 ACK를 받은 뒤 chat-service에 위임하는 내부 API를 추가한다.

| Item | Value |
| --- | --- |
| Method | `POST` |
| Path | `/internal/chat-rooms/{roomId}/read-receipts` |
| Header | `X-User-Id` |
| Request body | `requestId`, `type`, `lastReadSequence` |
| Success status | `202 Accepted` 또는 `200 OK` |

처리 흐름은 다음과 같다.

1. 방 존재 여부를 확인한다.
2. 요청 사용자가 active member인지 확인한다.
3. Redis에서 방 마지막 sequence를 조회한다.
4. 방 마지막 sequence가 Redis에 없으면 DB에서 조회하고 Redis에 저장한다.
5. `lastReadSequence`가 음수이면 validation error로 거절한다.
6. `lastReadSequence`가 방 마지막 sequence보다 크면 domain error로 거절한다.
7. 사용자 read sequence를 Redis hash에 단조 증가 방식으로 저장한다.
8. 실제 값이 증가한 경우 dirty set에 `{roomId}:{userId}`를 추가한다.
9. 클라이언트에는 처리된 `lastReadSequence`를 응답한다.

### Internal Message Send API Side Effect

기존 메시지 전송 API의 side effect를 확장한다.

| Item | Value |
| --- | --- |
| Method | `POST` |
| Path | `/internal/chat-rooms/{roomId}/messages` |
| Existing behavior | 메시지 저장, sequence 발급, Redis Stream 발행 |
| New behavior | 저장 commit 이후 room last sequence와 sender read sequence를 Redis에 반영 |

처리 흐름은 다음과 같다.

1. 메시지를 저장하고 sequence를 발급한다.
2. DB commit 이후 `stream:chat:message-created`를 발행한다.
3. DB commit 이후 `chat:room:{roomId}:last-sequence`를 message sequence까지 단조 증가 갱신한다.
4. DB commit 이후 sender의 `chat:user:{senderId}:room-read-sequences`를 message sequence까지 단조 증가 갱신한다.
5. sender read sequence가 증가하면 `chat:read-state:dirty`에 `{roomId}:{senderId}`를 추가한다.

## WebSocket/STOMP Design

### Client Send Message

기존 구조를 유지한다.

| Direction | Destination |
| --- | --- |
| Client SEND | `/app/chat-rooms/{roomId}/messages` |
| User ACK subscribe | `/user/queue/chat/acks` |
| Broadcast subscribe | `/topic/chat-rooms/{roomId}/messages` |
| Error subscribe | `/user/queue/chat/errors` |

메시지 전송 성공 ACK에는 기존 sequence를 유지한다. 클라이언트는 이 sequence를 사용해 본인 화면의 read cursor를 최신 상태로 볼 수 있다.

### Client Read Receipt

새 STOMP destination을 추가한다.

| Direction | Destination |
| --- | --- |
| Client SEND | `/app/chat-rooms/{roomId}/read-receipts` |
| User ACK subscribe | `/user/queue/chat/acks` |
| Error subscribe | `/user/queue/chat/errors` |

요청 payload는 다음 의미를 가진다.

| Field | Meaning |
| --- | --- |
| `requestId` | 클라이언트 요청 추적 ID |
| `type` | `CHAT_MESSAGES_READ` |
| `payload.lastReadSequence` | 사용자가 화면에서 읽었다고 확인한 마지막 메시지 sequence |
| `sentAt` | 클라이언트 전송 시각 |

성공 ACK payload는 다음 의미를 가진다.

| Field | Meaning |
| --- | --- |
| `requestId` | 원 요청 ID |
| `roomId` | 읽음 처리된 방 ID |
| `lastReadSequence` | 서버가 반영한 마지막 읽음 sequence |

읽음 ACK는 broadcast하지 않는다. 현재 요구사항은 개인의 unread count 계산이며, 다른 사용자에게 읽음 표시를 보여주는 기능은 범위 밖이다.

## Redis Stream Design

### Existing Message Created Stream

기존 Redis Stream을 유지한다.

| Item | Value |
| --- | --- |
| Key | `stream:chat:message-created` |
| Event type | `CHAT_MESSAGE_CREATED` |
| Producer | `chat-service` |
| Consumer | `gateway` instance별 consumer group |
| Purpose | 저장된 메시지를 WebSocket 구독자에게 broadcast |

Stream field는 기존 계약을 유지한다.

| Field | Meaning |
| --- | --- |
| `eventId` | event idempotency ID |
| `eventType` | `CHAT_MESSAGE_CREATED` |
| `aggregateId` | roomId 또는 message aggregate 식별자 |
| `occurredAt` | 이벤트 발생 시각 |
| `schemaVersion` | 이벤트 schema version |
| `payload` | 메시지 생성 payload JSON |
| `traceId` | tracing ID |

Payload는 기존 계약을 유지한다.

| Field | Meaning |
| --- | --- |
| `messageId` | 메시지 ID |
| `roomId` | 채팅방 ID |
| `senderId` | 전송자 ID |
| `sequence` | 방 단위 메시지 sequence |
| `type` | 메시지 타입 |
| `content` | 메시지 내용 |
| `createdAt` | 메시지 생성 시각 |

### Read Receipt Stream

읽음 처리를 위한 신규 Redis Stream은 만들지 않는다.

이유는 다음과 같다.

- 읽음 ACK는 같은 사용자의 같은 방에 대해 마지막 값만 중요하다.
- 모든 ACK 이벤트를 durable event로 쌓으면 중간 상태가 대부분 불필요한 write amplification이 된다.
- 30초 배치 DB 반영 요구에는 Redis hash와 dirty set이 더 단순하다.

향후 읽음 상태 변경을 다른 서비스가 소비해야 하거나 무손실 audit이 필요해지면 `stream:chat:read-state-updated`를 별도 설계한다.

## Batch Flush Design

### Scheduler

`chat-service`에 30초 주기 scheduler를 둔다.

| Item | Value |
| --- | --- |
| Interval | 30초 |
| Source key | `chat:read-state:dirty` |
| Target table | `chat_room_read_states` |
| Batch unit | dirty set member 목록 |

### Flush Flow

1. dirty set에서 일정 개수의 member를 읽는다.
2. 각 member를 `roomId`와 `userId`로 해석한다.
3. 각 `userId`별 Redis hash `chat:user:{userId}:room-read-sequences`에서 roomId field 값을 조회한다.
4. 조회된 최신 sequence만 DB batch upsert 대상으로 만든다.
5. DB upsert는 단조 증가 규칙으로 실행한다.
6. DB 반영이 성공한 member만 dirty set에서 제거한다.
7. Redis 조회 실패 또는 DB 오류가 발생한 member는 dirty set에 남겨 다음 주기에 재시도한다.

### Race Condition Handling

Flush 중 새 read ACK가 들어올 수 있다.

- flush가 sequence 10을 읽은 뒤 ACK sequence 12가 들어오면 Redis hash는 12로 증가하고 dirty set에는 같은 member가 유지된다.
- flush가 sequence 10을 DB에 반영하고 dirty set member를 제거하면 sequence 12가 dirty set에 이미 다시 추가되어 있어야 한다.
- 이를 보장하려면 read sequence 증가와 dirty set 추가를 원자 처리해야 한다.

dirty set 제거는 단순 제거만으로 race가 생길 수 있다. 구현 시 다음 중 하나를 선택한다.

- 제거 직전에 Redis hash의 현재 값이 flush한 sequence와 같은지 확인하고 같을 때만 dirty set에서 제거한다.
- 또는 dirty set 대신 dirty hash에 flush 대상 sequence를 함께 저장하고, 현재 sequence가 더 크면 dirty 상태를 유지한다.

권장 방식은 첫 번째다. 자료구조가 단순하고 현재 요구사항에 충분하다.

## Cache Warm-Up And Miss Recovery

### Room Last Sequence Miss

Redis에서 `chat:room:{roomId}:last-sequence`가 없으면 다음 순서로 복구한다.

1. DB에서 해당 방의 sequence 발급 상태를 조회한다.
2. `next_sequence`가 있으면 `lastMessageSequence = next_sequence - 1`로 계산한다.
3. sequence row가 없거나 메시지가 없는 방이면 `lastMessageSequence = 0`으로 본다.
4. 계산한 값을 Redis에 저장한다.
5. 이후 unread count 계산에 사용한다.

### User Read Sequence Miss

Redis hash에 사용자의 `{roomId}` field가 없으면 다음 순서로 복구한다.

1. DB의 `chat_room_read_states`에서 `(roomId, userId)`를 조회한다.
2. row가 있으면 `last_read_sequence`를 사용한다.
3. row가 없으면 `0`을 사용한다.
4. 값을 Redis hash에 저장한다.
5. 이 복구 저장은 dirty set에 추가하지 않는다. DB와 같은 값을 Redis에 적재하는 것이므로 DB flush 대상이 아니다.

### Combined Unread Count Calculation

채팅방 목록 조회 시 각 방에 대해 다음을 계산한다.

- `lastMessageSequence = Redis room last sequence or DB recovered value`
- `lastReadSequence = Redis user read sequence or DB recovered value`
- `unreadCount = max(0, lastMessageSequence - lastReadSequence)`

Redis에 저장된 사용자 read sequence가 DB보다 앞설 수 있으므로, 목록 조회는 Redis 값을 우선해야 한다.

## Failure Handling

### Redis Unavailable During Read ACK

읽음 ACK는 Redis에 즉시 반영되어야 하므로 Redis 장애 시 실패로 응답한다.

- 클라이언트는 재시도할 수 있다.
- DB에 직접 즉시 write하지 않는다. 이 기능의 쓰기 모델은 Redis write-behind로 통일한다.

### Redis Unavailable During Chat Room List

채팅방 목록 조회 중 Redis가 장애이면 DB 값으로 unread count를 계산한다.

- 이 경우 Redis에만 있고 DB에 아직 flush되지 않은 최근 30초 read state는 반영되지 않을 수 있다.
- 응답은 가능하면 성공시키되, 운영 로그와 metric에 cache fallback을 남긴다.

### Batch Flush Failure

DB upsert 실패 또는 Redis read 실패가 발생하면 dirty member를 제거하지 않는다.

- 다음 scheduler 주기에 재시도한다.
- 반복 실패를 운영 로그와 metric으로 남긴다.
- 실패 상세 Redis command나 SQL detail은 클라이언트에 노출하지 않는다.

## Validation And Authorization

읽음 ACK와 채팅방 목록 조회는 다음을 검증한다.

- 인증 사용자가 존재해야 한다.
- roomId는 존재하고 삭제되지 않은 방이어야 한다.
- 사용자는 해당 방의 active member여야 한다.
- `lastReadSequence`는 0 이상이어야 한다.
- `lastReadSequence`는 현재 방의 `lastMessageSequence`보다 클 수 없다.

`lastReadSequence`가 현재 방 마지막 sequence보다 큰 경우는 거절한다. 서버가 임의로 clamp하면 클라이언트 버그나 다른 방 sequence 혼입을 숨길 수 있다.

## Concurrency Model

전체 기능은 다음 불변식을 지킨다.

- 방의 마지막 sequence는 뒤로 가지 않는다.
- 사용자의 마지막 읽은 sequence는 뒤로 가지 않는다.
- DB에 저장된 읽음 sequence는 뒤로 가지 않는다.
- unread count는 음수가 될 수 없다.

동시성 시나리오는 다음처럼 처리한다.

| Scenario | Handling |
| --- | --- |
| sequence 20 ACK 후 sequence 15 ACK가 늦게 도착 | Redis hash가 `max` 갱신하므로 20 유지 |
| 메시지 생성 이벤트가 순서 없이 Redis last sequence를 갱신 | Redis String이 `max` 갱신하므로 큰 sequence 유지 |
| batch flush가 같은 member를 중복 처리 | DB upsert가 `max` 갱신하므로 안전 |
| flush 도중 새 ACK가 들어옴 | read update와 dirty 추가를 원자 처리하고, dirty 제거 전 현재 sequence를 재확인 |
| sender가 메시지를 보내고 동시에 read ACK를 보냄 | 둘 다 `max` 갱신하므로 더 큰 sequence 유지 |

## Implementation Tasks

### Task 1: Add Persistent Read State Model

**Files:**

- Create migration for `chat_room_read_states`
- Create read state domain/entity model in `chat-service`
- Create repository with lookup and batch upsert support

- [ ] Add DB schema for `(room_id, member_id, last_read_sequence, created_at, updated_at)`.
- [ ] Add constraints and indexes described in this plan.
- [ ] Add repository behavior for single lookup by `(roomId, memberId)`.
- [ ] Add repository behavior for batch upsert using monotonic `max` update.
- [ ] Add tests for insert, monotonic update, and stale update ignored behavior.

### Task 2: Add Redis Read State Store

**Files:**

- Create Redis adapter for room last sequence.
- Create Redis adapter for user read sequence.
- Create Redis adapter for dirty read state tracking.

- [ ] Implement room last sequence read, monotonic update, and DB miss warm-up orchestration boundary.
- [ ] Implement user read sequence read, monotonic update, and dirty set marking.
- [ ] Ensure user read sequence increase and dirty set marking are atomic.
- [ ] Ensure room last sequence update is monotonic.
- [ ] Add Redis adapter tests for stale sequence, newer sequence, missing value, and dirty marking behavior.

### Task 3: Extend Message Send Side Effects

**Files:**

- Modify `chat-service` message send application flow.
- Update message service tests.

- [ ] After message DB commit, update `chat:room:{roomId}:last-sequence` with message sequence.
- [ ] After message DB commit, update sender read sequence with message sequence.
- [ ] Mark sender read state dirty only when sender read sequence increased.
- [ ] Keep existing `stream:chat:message-created` event contract unchanged.
- [ ] Add tests proving sender's own message does not become unread.

### Task 4: Add Read Receipt API In Chat Service

**Files:**

- Create internal REST request/response DTOs.
- Add internal REST endpoint.
- Add application service method for read receipt.
- Add controller and service tests.

- [ ] Accept `lastReadSequence` for `(roomId, userId)`.
- [ ] Validate active room and active membership.
- [ ] Load room last sequence from Redis, falling back to DB and warming Redis on miss.
- [ ] Reject negative sequence.
- [ ] Reject sequence greater than room last sequence.
- [ ] Monotonically update Redis user read sequence and mark dirty.
- [ ] Return accepted read sequence to gateway.

### Task 5: Add Gateway STOMP Read Receipt Handler

**Files:**

- Create STOMP read receipt request DTO.
- Create STOMP read receipt ACK payload.
- Extend gateway chat message client or add read receipt client.
- Add STOMP controller method.
- Add STOMP controller tests.

- [ ] Add `SEND /app/chat-rooms/{roomId}/read-receipts`.
- [ ] Read authenticated principal from STOMP session.
- [ ] Forward read receipt to chat-service internal REST API.
- [ ] Return user-specific ACK to `/user/queue/chat/acks`.
- [ ] Return validation or domain errors through `/user/queue/chat/errors`.

### Task 6: Add Unread Count To Chat Room List

**Files:**

- Modify chat room summary DTO.
- Modify chat room listing application flow.
- Add tests for Redis hit, Redis miss, DB fallback, and unread count calculation.

- [ ] Add `lastMessageSequence`, `lastReadSequence`, and `unreadCount` to room summary response.
- [ ] For listed rooms, resolve room last sequence from Redis or DB warm-up.
- [ ] Resolve current user's read sequence from Redis or DB warm-up.
- [ ] Calculate `unreadCount = max(0, lastMessageSequence - lastReadSequence)`.
- [ ] Ensure joined room scope only exposes unread state for rooms visible to the authenticated user.

### Task 7: Add 30-Second Batch Flush Scheduler

**Files:**

- Create batch flush scheduler.
- Create configurable properties for interval and batch size.
- Add scheduler tests with mocked Redis and repository.

- [ ] Read dirty entries from `chat:read-state:dirty`.
- [ ] Load latest read sequence for each dirty entry from Redis.
- [ ] Batch upsert latest read states into PostgreSQL.
- [ ] Remove dirty entry only after DB success and only if Redis still equals the flushed sequence.
- [ ] Leave failed entries dirty for retry.
- [ ] Emit logs and metrics for flush count, failure count, and skipped stale removals.

### Task 8: Update Documentation

**Files:**

- Update `docs/services/chat-service/features/chat-messages.md`.
- Update `docs/services/gateway/features/local-session-and-stomp-gateway.md`.
- Add or update service operations docs if scheduler properties are introduced.

- [ ] Document read receipt flow.
- [ ] Document unread count calculation.
- [ ] Document Redis keys, values, and no-TTL decision.
- [ ] Document DB write-behind and 30초 유실 가능성 trade-off.
- [ ] Document WebSocket destination and internal REST contract.
- [ ] Document that no new read receipt Redis Stream is introduced.

## Verification Plan

다음 동작을 테스트로 검증한다.

- 메시지 전송자가 보낸 메시지는 본인 unread count에 포함되지 않는다.
- 오래된 read ACK가 최신 read sequence를 낮추지 못한다.
- Redis room last sequence miss 시 DB에서 복구하고 Redis에 저장한다.
- Redis user read sequence miss 시 DB에서 복구하고 Redis에 저장한다.
- Redis에만 최신 read state가 있으면 채팅방 목록은 Redis 값을 우선한다.
- 30초 batch flush는 dirty entry를 DB에 upsert한다.
- batch flush 실패 시 dirty entry는 제거되지 않는다.
- flush 도중 더 최신 sequence가 들어오면 dirty entry가 잘못 제거되지 않는다.
- 권한 없는 사용자의 read ACK는 거절된다.
- `lastReadSequence`가 방 마지막 sequence보다 크면 거절된다.

## Design Decisions

- 읽음 상태는 Redis에 즉시 반영하고 DB는 30초 단위로 batch flush한다.
- Redis miss는 DB에서 복구한 뒤 Redis에 warm-up한다.
- Redis 장애 중 read ACK는 실패 처리한다.
- Redis 장애 중 채팅방 목록은 DB fallback으로 응답한다.
- sender read sequence는 메시지 저장 commit 이후 자동으로 메시지 sequence까지 올린다.
- unread count는 `lastMessageSequence - lastReadSequence`로 계산하고 음수는 0으로 보정한다.
- 읽음 ACK broadcast는 만들지 않는다.
- 읽음 처리를 위한 신규 Redis Stream은 만들지 않는다.

