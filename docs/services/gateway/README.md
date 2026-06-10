# Gateway Service

이 디렉터리는 `gateway` 서비스의 기능, 내부 구조, 운영 절차, 서비스 전용 설계 결정을 기록한다.

## Directory Structure

- `features/`: gateway 기능 단위 문서.
- `architecture/`: gateway 내부 구조와 요청 처리 흐름.
- `adr/`: gateway 내부에 한정되는 설계 결정.
- `operations/`: gateway 로컬 실행, 설정, 장애 대응, 트러블슈팅.

## Document Rules

- gateway 내부 변경은 이 디렉터리 아래에 문서화한다.
- 다른 서비스와의 계약이나 흐름이 바뀌면 루트 `docs/architecture/` 또는 `docs/adr/`도 함께 갱신한다.
- 기능 문서는 `features/_template.md`를 복사해 작성한다.
