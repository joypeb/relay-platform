# Project Documentation

이 디렉터리는 기능, 아키텍처 결정, 운영 절차를 코드와 함께 추적하기 위한 문서 공간이다.

목표는 다른 개발자가 문서만 먼저 읽어도 기능 흐름과 관련 코드를 빠르게 이해할 수 있게 하는 것이다.

## Directory Structure

- `services/<service-name>/`: 서비스별 문서. 기능, 내부 구조, 서비스 전용 ADR, 운영 문서를 포함한다.
- `services/_template/`: 새 서비스 문서 디렉터리를 만들 때 사용하는 템플릿.
- `architecture/`: 여러 서비스에 걸친 시스템 흐름, 서비스 경계, 통신 방식 같은 전역 아키텍처 문서.
- `adr/`: 여러 서비스에 영향을 주는 중요한 설계 결정을 남기는 전역 Architecture Decision Record.

서비스가 별도 프로젝트로 분리되어 있어도 같은 구조를 각 서비스 저장소에 둘 수 있다. 이 저장소에서는 현재 `gateway` 서비스를 `services/gateway/` 아래에 문서화한다.

## When To Update

다음 변경이 있으면 관련 문서를 추가하거나 갱신한다.

- 새로운 기능을 추가한 경우
- 기존 기능의 핵심 로직이 바뀐 경우
- REST API, WebSocket/STOMP destination, Redis key/stream, DB schema 같은 외부 계약이 바뀐 경우
- 서비스 경계, 트랜잭션, 장애 처리, 재시도 정책 같은 설계 결정이 바뀐 경우
- 실행 방법, 설정, 운영 절차가 바뀐 경우

## Feature Document Checklist

기능 문서는 최소한 다음 내용을 포함한다.

1. 기능 목적
2. 전체 처리 흐름
3. 핵심 로직과 주요 분기
4. 관련 코드 파일 경로
5. API, WebSocket, Redis, DB 등 외부 계약
6. 실패 처리와 예외 상황
7. 테스트 및 검증 방법
8. 설계 결정과 trade-off

서비스별 새 기능 문서는 `services/<service-name>/features/_template.md`를 복사해 작성한다. 새 서비스가 추가되면 `services/_template/` 디렉터리를 복사해 `services/<service-name>/`을 만든다.
