# Redis and Redis Streams Rules

## Scope

- 이 규칙은 Redis key 설계, cache/session/presence 용도, Redis Streams event 설계, consumer group 처리 기준을 정의한다.
- Redis는 빠른 상태, cache, pub/sub, stream 처리에 사용한다. PostgreSQL을 대체하는 영구 원천 저장소로 사용하지 않는다.

## Redis Key Naming

- key는 lower kebab-case 또는 lower snake case가 아니라 colon-delimited namespace를 사용한다.
- 기본 형식은 `<service>:<domain>:<resource>:<id>:<purpose>`이다.
- key에 포함되는 ID는 UUID, ULID, database id 중 하나로 일관되게 사용한다.

```text
chat:room:{roomId}:members
chat:room:{roomId}:presence
chat:user:{userId}:sessions
chat:message:{messageId}:dedupe
```

## TTL

- cache, presence, lock, dedupe key는 TTL을 반드시 명시한다.
- 영구 보존이 필요한 데이터는 Redis에만 저장하지 않는다.
- TTL 값은 코드 상수와 설정값 중 하나로 이름을 붙여 관리한다.
- TTL 없는 key를 추가할 때는 이유를 주석 또는 설계 문서에 남긴다.

## Serialization

- Redis value는 JSON 또는 String serializer를 기본으로 한다.
- Java native serialization은 사용하지 않는다.
- Redis에 저장하는 DTO는 schema evolution을 고려해 optional field 추가에 안전해야 한다.
- key와 hash field는 사람이 읽을 수 있는 문자열로 직렬화한다.

## Redis Streams

- stream key는 `stream:<domain>:<event>` 형식을 사용한다.
- consumer group은 `<service-name>-group` 형식을 사용한다.
- consumer name은 `<service-name>-<instance-id>` 형식을 사용한다.
- stream event는 `eventId`, `eventType`, `aggregateId`, `occurredAt`, `schemaVersion`, `payload`, `traceId`를 포함한다.

```text
stream:chat:message-created
stream:chat:room-member-changed
```

## Consumer Processing

- consumer는 at-least-once delivery를 전제로 idempotent하게 구현한다.
- 처리 성공 후에만 ACK 한다.
- pending entry list를 주기적으로 확인하고 재처리 정책을 둔다.
- dead letter stream은 `stream:<domain>:<event>:dlq` 형식을 사용한다.
- poison message는 retry count, last error, failedAt을 기록한 뒤 DLQ로 이동한다.
- `XREADGROUP` 사용 시 `COUNT`, `BLOCK` 값을 명시한다.

## Consistency

- DB 저장과 Redis Stream publish가 함께 필요한 경우 transactional outbox 또는 실패 보상 전략을 사용한다.
- Redis Stream message id를 비즈니스 id로 사용하지 않는다.
- business idempotency는 별도 `eventId` 또는 domain command id로 판단한다.

## 금지

- `KEYS` 명령을 운영 경로에서 사용하지 않는다.
- Redis key scan 결과에 비즈니스 정합성을 의존하지 않는다.
- `NOACK`을 기본값으로 사용하지 않는다.
- consumer 오류를 catch 후 로그만 남기고 ACK 하지 않는다.
- Java native serialization을 사용하지 않는다.

## 예외

- ephemeral local development key는 짧은 TTL과 명확한 prefix가 있으면 단순화할 수 있다.
- fire-and-forget 알림처럼 손실 허용이 명시된 event는 Redis Pub/Sub을 사용할 수 있다.

## 점검 기준

- 신규 key의 namespace, TTL, serializer가 문서화되어 있는지 확인한다.
- Stream consumer가 중복 수신, 재처리, DLQ를 처리하는지 확인한다.
- Redis 장애 시 핵심 데이터 손실이 없는지 확인한다.
- Redis operation에 timeout과 error log가 있는지 확인한다.

## 근거 문서

- Redis Streams: https://redis.io/docs/latest/develop/data-types/streams/
- Redis XREADGROUP: https://redis.io/docs/latest/commands/xreadgroup/
- Spring Data Redis RedisTemplate: https://docs.spring.io/spring-data/redis/reference/redis/template.html
- Spring Data Redis Streams: https://docs.spring.io/spring-data/redis/reference/redis/redis-streams.html
