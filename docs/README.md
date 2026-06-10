# Project Documentation

이 디렉터리는 기능, 아키텍처 결정, 운영 절차를 코드와 함께 추적하기 위한 문서 공간이다.

목표는 다른 개발자가 문서만 먼저 읽어도 기능 흐름과 관련 코드를 빠르게 이해할 수 있게 하는 것이다.

## Directory Structure

- `features/`: 기능 단위 문서. 기능의 목적, 처리 흐름, 핵심 로직, 관련 코드를 설명한다.
- `architecture/`: 서비스 경계, 모듈 구조, 시스템 흐름 같은 아키텍처 문서.
- `adr/`: 중요한 설계 결정을 남기는 Architecture Decision Record.
- `operations/`: 로컬 실행, 배포, 장애 대응, 트러블슈팅 문서.

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

새 기능 문서는 `features/_template.md`를 복사해 작성한다.
