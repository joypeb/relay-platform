# chat-service Architecture Overview

## 역할

`chat-service`는 채팅방 도메인의 원천 데이터를 소유한다. Gateway는 인증과 연결 경계를 담당하고, `chat-service`는 전달받은 사용자 식별자를 기준으로 채팅방 비즈니스 규칙을 수행한다.

## 내부 구조

```text
com.joypeb.chatservice
  chatroom
    api
    application
    domain
    dto
    infrastructure
  common
    api
    config
    error
```

- `api`: REST endpoint, request validation, `X-User-Id` 추출
- `application`: use case orchestration, transaction boundary
- `domain`: `ChatRoom`, `ChatRoomMember`와 enum 기반 도메인 상태
- `dto`: REST request/response 모델
- `infrastructure`: Spring Data JPA repository
- `common`: 공통 응답, `Clock`, REST 오류 변환

## 데이터 소유권

`chat-service`는 다음 테이블을 직접 소유한다.

- `chat_rooms`
- `chat_room_members`

다른 서비스는 이 테이블을 직접 조회하거나 수정하지 않고, REST API 또는 이후 추가될 이벤트 계약을 통해 접근해야 한다.

## 현재 제외 범위

- 메시지 저장
- WebSocket/STOMP destination
- Redis publish 또는 Redis Streams
- 채팅방 초대, 참가, 나가기 API
- 읽음 처리
