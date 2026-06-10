# JPA and PostgreSQL Rules

## Scope

- 이 규칙은 Spring Data JPA, transaction, entity mapping, repository, PostgreSQL schema 설계를 정의한다.
- PostgreSQL은 영구 데이터의 원천 저장소로 사용한다.

## Entity

- Entity는 domain invariant를 보호해야 한다.
- Entity 생성은 constructor, static factory, builder 중 하나로 제한한다.
- public setter는 사용하지 않는다. 상태 변경은 의도가 드러나는 메서드로 제공한다.
- `@ManyToOne`, `@OneToOne`, `@OneToMany`, `@ManyToMany`는 기본적으로 LAZY loading을 사용한다.
- `equals`와 `hashCode`는 JPA lifecycle과 proxy를 고려해 작성한다.
- Entity는 REST DTO, STOMP DTO, Redis Stream DTO로 직접 사용하지 않는다.

## Repository

- repository interface는 aggregate root 단위로 만든다.
- 단순 CRUD와 조건 조회는 Spring Data JPA query method를 사용한다.
- 복잡한 동적 조회는 Specification, QueryDSL, explicit query 중 하나를 사용한다.
- 조회 전용 projection은 Entity 전체 loading 대신 DTO projection 또는 interface projection을 우선 검토한다.
- N+1 가능성이 있는 조회는 fetch join, entity graph, batch size 중 하나로 해결한다.

## Transaction

- transaction boundary는 application service 계층에 둔다.
- write use case는 `@Transactional`을 명시한다.
- read-only use case는 `@Transactional(readOnly = true)`를 사용한다.
- Controller, WebSocket handler, repository helper에 transaction boundary를 두지 않는다.
- 외부 HTTP 호출, STOMP 전송, Redis publish를 DB transaction 안에서 장시간 수행하지 않는다.
- transaction 안에서 event 발행이 필요하면 outbox 또는 transaction synchronization 정책을 명시한다.

## PostgreSQL Schema

- 모든 주요 테이블은 primary key를 가진다.
- foreign key, unique, check constraint는 DB schema에도 명시한다.
- 자주 조회되는 foreign key와 검색 조건에는 index를 둔다.
- `created_at`, `updated_at`은 주요 mutable table에 둔다.
- soft delete를 사용할 때는 `deleted_at`과 partial unique index 필요 여부를 함께 검토한다.
- migration은 Flyway 또는 Liquibase 같은 versioned migration 도구로 관리한다.

## Naming

- table과 column은 lower snake_case를 사용한다.
- primary key column은 `id` 또는 `<table>_id` 중 하나로 프로젝트 전체에서 통일한다.
- foreign key column은 `<referenced_resource>_id` 형식을 사용한다.
- index 이름은 `idx_<table>_<column...>` 형식을 사용한다.
- unique constraint 이름은 `uk_<table>_<column...>` 형식을 사용한다.

## Locking and Concurrency

- 동시 수정 충돌 가능성이 있는 aggregate는 optimistic locking을 우선 검토한다.
- inventory, membership limit, once-only command처럼 경쟁 조건이 있는 use case는 DB constraint와 lock 전략을 함께 설계한다.
- `SELECT FOR UPDATE` 같은 pessimistic lock은 transaction 범위와 timeout을 명시한다.

## 금지

- Open Session in View에 의존해 Controller에서 lazy loading을 발생시키지 않는다.
- Entity graph 없이 collection 연관을 무제한 직렬화하지 않는다.
- migration 없이 운영 DB schema를 수동 변경하지 않는다.
- database constraint 없이 application validation만으로 unique/integrity를 보장하지 않는다.
- `@Transactional` self-invocation에 의존하지 않는다.

## 예외

- 단순 reference table은 aggregate root repository 없이 조회 전용 repository를 둘 수 있다.
- read-heavy projection은 별도 query model 또는 materialized view를 사용할 수 있다.

## 점검 기준

- Entity가 DTO로 직접 노출되지 않는지 확인한다.
- transaction boundary가 application service에 있는지 확인한다.
- schema constraint가 domain invariant와 일치하는지 확인한다.
- collection 조회 API에서 N+1과 pagination 문제가 없는지 확인한다.

## 근거 문서

- Spring Data JPA Reference: https://docs.spring.io/spring-data/jpa/reference/jpa.html
- Spring Framework Transaction Management: https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html
- Spring Boot SQL Databases: https://docs.spring.io/spring-boot/reference/data/sql.html
- PostgreSQL Constraints: https://www.postgresql.org/docs/current/ddl-constraints.html
- PostgreSQL Indexes: https://www.postgresql.org/docs/current/indexes.html
