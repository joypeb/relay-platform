# chat-service

`chat-service`는 채팅 도메인의 원천 데이터를 소유하는 Spring Boot 서비스다. 현재는 채팅방과 채팅방 멤버십을 PostgreSQL에 저장하고, REST API로 채팅방 생성, 목록 조회, 상세 조회, 수정, 삭제를 제공한다.

## 주요 책임

- 채팅방 생성, 조회, 수정, 삭제
- 채팅방 생성자를 OWNER 멤버로 등록
- OWNER 기준 수정/삭제 권한 검증
- soft delete 기반 채팅방 삭제

## 외부 계약

- REST API: `/api/v1/chat-rooms`
- 사용자 식별 header: `X-User-Id`
- DB tables: `chat_rooms`, `chat_room_members`

## Related Documents

- `docs/services/chat-service/features/chat-rooms.md`
- `docs/services/chat-service/architecture/overview.md`
- `docs/services/chat-service/operations/local-run.md`
