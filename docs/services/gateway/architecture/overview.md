# Gateway Architecture Overview

이 문서는 `gateway` 서비스의 내부 구조와 요청 처리 흐름을 설명한다.

## Responsibility

- 서비스 책임:
- 외부에 제공하는 계약:
- 라우팅 대상 서비스:
- 의존하는 외부 시스템:

## Package Or Module Structure

- `gateway/src/main/java/com/joypeb/gateway`: gateway 애플리케이션 코드.
- `gateway/src/main/resources`: gateway 설정.
- `gateway/src/test`: gateway 테스트.

## Main Flows

### Request Routing

1. 클라이언트 요청이 gateway로 들어온다.
2. gateway가 인증, 공통 필터, 라우팅 정책을 적용한다.
3. 대상 서비스로 요청을 전달한다.
4. 대상 서비스 응답을 클라이언트에 반환한다.

## Cross-Cutting Concerns

- 인증/인가:
- 로깅:
- tracing:
- metrics:
- health check:
- 오류 응답:
