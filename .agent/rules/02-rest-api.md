# REST API Rules

## Scope

- 이 규칙은 Spring Web MVC 기반 REST API의 URL, HTTP method, response, error, validation 설계를 정의한다.
- Actuator endpoint와 WebSocket endpoint는 이 규칙의 공통 응답 포맷 적용 대상이 아니다.

## URL

- public API prefix는 `/api/v1`을 사용한다.
- resource name은 복수형 kebab-case 명사를 사용한다.
- URL에는 동사를 넣지 않는다.
- resource 계층은 소유 관계가 명확할 때만 중첩한다.
- path variable 이름은 domain 용어를 사용한다. 예: `{roomId}`, `{messageId}`, `{memberId}`

```text
GET    /api/v1/chat-rooms
POST   /api/v1/chat-rooms
GET    /api/v1/chat-rooms/{roomId}
GET    /api/v1/chat-rooms/{roomId}/messages
POST   /api/v1/chat-rooms/{roomId}/messages
PATCH  /api/v1/chat-rooms/{roomId}/members/{memberId}
DELETE /api/v1/chat-rooms/{roomId}/members/{memberId}
```

## HTTP Method

- 조회는 `GET`을 사용한다.
- 생성은 `POST`를 사용한다.
- 전체 교체는 `PUT`을 사용한다.
- 부분 수정은 `PATCH`를 사용한다.
- 삭제는 `DELETE`를 사용한다.
- 비멱등 command는 `POST`를 사용하고, command 의미를 sub-resource로 표현한다.

## Response

- 성공 응답은 다음 wrapper를 기준으로 한다.

```json
{
  "success": true,
  "data": {},
  "traceId": "trace-id",
  "timestamp": "2026-06-10T10:00:00Z"
}
```

- collection 응답은 pagination metadata를 포함한다.
- 생성 응답은 `201 Created`와 생성된 resource identifier를 반환한다.
- 삭제 성공이고 반환할 body가 없으면 `204 No Content`를 사용한다.
- Controller는 `ResponseEntity`로 status code를 명시한다.

## Error Response

- 오류 응답은 RFC 9457 `ProblemDetail` 또는 그와 호환되는 구조를 사용한다.

```json
{
  "type": "https://example.com/problems/chat-room-not-found",
  "title": "Chat room not found",
  "status": 404,
  "detail": "Chat room was not found.",
  "instance": "/api/v1/chat-rooms/123",
  "code": "CHAT_ROOM_NOT_FOUND",
  "traceId": "trace-id"
}
```

- 예외 변환은 `@RestControllerAdvice`에서 중앙 처리한다.
- validation failure는 field-level error를 포함한다.
- 내부 예외 class name, stack trace, SQL, Redis command detail은 client에 노출하지 않는다.

## Status Code

- `200 OK`: 조회, 수정 응답 body 반환
- `201 Created`: 생성 성공
- `202 Accepted`: 비동기 command 접수
- `204 No Content`: body 없는 삭제/처리 성공
- `400 Bad Request`: request validation 실패
- `401 Unauthorized`: 인증 실패
- `403 Forbidden`: 권한 실패
- `404 Not Found`: resource 없음
- `409 Conflict`: 현재 상태와 충돌
- `422 Unprocessable Content`: 문법은 맞지만 도메인 규칙 위반
- `500 Internal Server Error`: 예상하지 못한 서버 오류

## 금지

- Entity를 직접 request/response body로 사용하지 않는다.
- `200 OK`로 모든 성공/실패를 표현하지 않는다.
- query parameter에 JSON 문자열을 넣지 않는다.
- controller method에서 `try-catch`로 error response를 직접 만들지 않는다.
- page 번호, size, sort의 기본값과 최대값 없이 collection API를 노출하지 않는다.

## 예외

- health, metrics, actuator, OpenAPI endpoint는 Spring Boot 또는 도구 기본 포맷을 따른다.
- file download, SSE, streaming response는 wrapper를 적용하지 않을 수 있다.

## 점검 기준

- URL이 resource 중심이고 kebab-case인지 확인한다.
- request DTO에 Bean Validation이 선언되어 있는지 확인한다.
- response DTO가 Entity와 분리되어 있는지 확인한다.
- 모든 business exception이 `ProblemDetail` 또는 공통 error contract로 변환되는지 확인한다.

## 근거 문서

- Spring Web MVC Annotated Controllers: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller.html
- Spring Web MVC Error Responses: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html
- Spring Web MVC Validation: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-validation.html
- Spring Boot Web: https://docs.spring.io/spring-boot/reference/web/servlet.html
