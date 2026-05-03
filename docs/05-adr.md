# ADR (Architecture Decision Record)

> ARTINUS Subscription API 주요 기술 선택과 설계 결정 이유

## 목차

- [ADR-001. 멀티모듈 + 헥사고날 아키텍처](#adr-001-멀티모듈--헥사고날-아키텍처)
- [ADR-002. MySQL + Flyway + JPA](#adr-002-mysql--flyway--jpa)
- [ADR-003. DB 기반 멱등성 처리](#adr-003-db-기반-멱등성-처리)
- [ADR-004. 낙관적 락 기반 동시성 제어](#adr-004-낙관적-락-기반-동시성-제어)
- [ADR-005. CSRNG 외부 API 동기 호출](#adr-005-csrng-외부-api-동기-호출)
- [ADR-006. Resilience4j timeout / retry / circuit breaker](#adr-006-resilience4j-timeout--retry--circuit-breaker)
- [ADR-007. OpenAI LLM 요약 + fallback](#adr-007-openai-llm-요약--fallback)
- [ADR-008. Redis와 비동기 전환을 제외한 이유](#adr-008-redis와-비동기-전환을-제외한-이유)
- [ADR-009. Testcontainers와 k6 검증](#adr-009-testcontainers와-k6-검증)

---

## ADR-001. 멀티모듈 + 헥사고날 아키텍처

### 결정

프로젝트를 `domain`, `application`, `infrastructure`, `api`, `app` 모듈로 분리하고, application 계층은 port를 통해 외부 구현을 바라보도록 구성했다.

### 이유

- 도메인 규칙과 외부 기술(JPA, CSRNG, OpenAI)을 분리하기 위해서다.
- 구독 상태 전이, 채널 정책, 멱등성 같은 핵심 규칙을 웹/DB 구현과 독립적으로 테스트할 수 있다.
- 외부 API나 영속성 구현이 바뀌어도 application usecase의 변경 범위를 줄일 수 있다.

### 대안

- 단일 모듈 layered architecture
- 단순 CRUD 구조

### 결과

- 초기 구조는 조금 더 복잡하지만, 과제의 도메인/외부 API/LLM/장애 대응 요구사항을 계층별로 분리해 설명하기 쉬워졌다.

---

## ADR-002. MySQL + Flyway + JPA

### 결정

운영 DB는 MySQL을 기준으로 하고, schema는 Flyway migration으로 관리한다. 애플리케이션 영속성 구현은 Spring Data JPA를 사용한다.

### 이유

- 구독 상태, 이력, 멱등성 데이터는 관계형 모델에 잘 맞는다.
- Flyway를 사용하면 schema와 seed data를 코드 저장소에서 추적할 수 있다.
- JPA의 `@Version`을 활용해 낙관적 락을 간결하게 구현할 수 있다.

### 대안

- H2 단독 사용
- MyBatis/JdbcTemplate
- NoSQL

### 결과

- 로컬 실행은 Docker MySQL, 테스트는 MySQL Testcontainers로 구성했다.
- H2는 일부 테스트 리소스에서만 사용하고, DB 통합 테스트는 MySQL 기준으로 검증한다.

---

## ADR-003. DB 기반 멱등성 처리

### 결정

`idempotency_keys` 테이블에 `(phone_number, idempotency_key)` 유니크 제약을 두고, 요청 해시와 완료 응답을 저장한다.

### 이유

- 같은 요청 재시도 시 저장된 응답을 재사용할 수 있다.
- 같은 키로 다른 요청 본문이 들어오면 충돌로 판단할 수 있다.
- 현재 규모에서는 Redis 없이도 DB unique constraint로 충분히 일관성을 확보할 수 있다.

### 대안

- Redis TTL 기반 멱등성 저장소
- 클라이언트 재시도만 허용하고 서버 멱등성 미구현

### 결과

- 성공한 요청만 완료 결과를 저장한다.
- CSRNG 거절이나 외부 API 장애처럼 완료되지 않은 요청은 같은 키로 재시도할 수 있다.

---

## ADR-004. 낙관적 락 기반 동시성 제어

### 결정

`members.version` 컬럼과 JPA `@Version`으로 회원 구독 상태 변경의 동시성 충돌을 감지한다.

### 이유

- 구독 상태 변경은 단일 회원 row에 대한 쓰기 충돌 문제다.
- 비관적 락보다 구현이 단순하고, 충돌 빈도가 높지 않은 일반적인 API 요청에 적합하다.
- 충돌 시 클라이언트가 최신 상태를 다시 조회하고 재시도하면 된다.

### 대안

- 비관적 락
- Redis 분산 락
- DB isolation level 조정

### 결과

- 동시 수정 충돌은 `409 Conflict`로 응답한다.
- Redis 없이 DB 레벨에서 일관성을 유지한다.

---

## ADR-005. CSRNG 외부 API 동기 호출

### 결정

구독/해지 명령 처리 중 CSRNG 외부 승인 API를 동기 호출한다.

### 이유

- 요구사항이 외부 API 응답에 따라 트랜잭션 커밋/롤백을 결정하도록 되어 있다.
- 클라이언트는 구독/해지 요청의 최종 성공 여부를 즉시 받아야 한다.
- 비동기 `PENDING` 상태를 도입하면 API 계약과 도메인 흐름이 커진다.

### 대안

- 요청 접수 후 `PENDING` 저장, 외부 승인 비동기 처리
- Outbox + Scheduler 보정

### 결과

- 동기 호출의 지연/장애 영향을 줄이기 위해 timeout, retry, circuit breaker를 적용했다.
- 비동기 전환은 트래픽 증가 또는 외부 API 지연이 커지는 시점의 확장안으로 남긴다.

---

## ADR-006. Resilience4j timeout / retry / circuit breaker

### 결정

CSRNG와 OpenAI 외부 API 호출에 timeout, retry, circuit breaker를 적용한다.

### 이유

- 외부 API 지연이 WAS 요청 스레드를 오래 점유하지 않도록 해야 한다.
- 일시적인 네트워크 실패는 짧게 재시도할 가치가 있다.
- 장애율이 높아지면 circuit breaker로 빠르게 실패시켜 내부 자원을 보호한다.

### 대안

- RestClient 기본 설정만 사용
- 직접 retry 로직 구현
- Spring Retry 사용

### 결과

- 외부 호출 공통 래퍼 `ExternalApiResilience`를 두었다.
- 비즈니스 거절(`random=0`)은 retry로 성공 보정하지 않는다.
- OpenAI 장애는 fallback 요약으로 처리한다.

---

## ADR-007. OpenAI LLM 요약 + fallback

### 결정

구독 이력 조회 시 DB에서 이력을 먼저 조회한 뒤, 해당 이력 목록을 OpenAI Responses API에 전달해 자연어 요약을 생성한다. LLM 비활성화 또는 실패 시 fallback 요약을 반환한다.

### 이유

- LLM은 DB를 직접 조회하지 않고, 애플리케이션이 선별한 이력 데이터만 요약해야 한다.
- API key가 없거나 외부 LLM 장애가 있어도 이력 조회 자체는 정상 동작해야 한다.
- 기본값을 `LLM_ENABLED=false`로 두어 로컬 실행과 테스트에서 비용이 발생하지 않게 한다.

### 대안

- 항상 fallback 요약만 사용
- LLM 응답을 DB에 캐싱
- 비동기 요약 생성 후 저장

### 결과

- OpenAI 호출 로그는 model, historyCount, summaryLength만 남긴다.
- API key, 전화번호, 요청 본문은 로그에 남기지 않는다.

---

## ADR-008. Redis와 비동기 전환을 제외한 이유

### 결정

현재 구현 범위에서는 Redis, 메시지 큐, Outbox 기반 비동기 전환을 도입하지 않는다.

### 이유

- 멱등성은 DB unique constraint로 처리 가능하다.
- 동시성은 `members.version` 낙관적 락으로 충분하다.
- 구독/해지 요청은 외부 승인 결과에 따라 즉시 성공/실패를 반환해야 한다.
- Redis나 메시지 큐를 도입하면 운영 구성과 장애 지점이 늘어난다.

### 대안

- Redis TTL 기반 멱등성 저장
- Redis 분산 락
- Outbox + Scheduler + PENDING 상태
- SQS/Kafka 기반 비동기 처리

### 결과

- 현재 구조는 MySQL 중심으로 단순하게 유지한다.
- Redis, 비동기 전환, 아카이빙은 트래픽 증가나 운영 보관 정책 확정 이후 확장한다.

---

## ADR-009. Testcontainers와 k6 검증

### 결정

DB 통합 테스트는 MySQL Testcontainers로 실행하고, 최종 수동 검증에는 k6 스모크 부하 테스트를 포함한다.

### 이유

- H2는 MySQL과 SQL/DDL 차이가 있어 실제 운영 DB와 다른 결과가 나올 수 있다.
- Testcontainers는 로컬/CI에서 실제 MySQL 기반 검증을 제공한다.
- k6는 API 응답 안정성과 기본 지연 시간을 간단히 확인하기 좋다.

### 대안

- H2 단독 테스트
- 수동 curl 검증만 수행
- 별도 성능 테스트 생략

### 결과

- `./gradlew test`로 단위/웹/API/DB 통합 테스트를 실행한다.
- k6 스모크는 외부 CSRNG와 DB 상태 변경이 없는 health, 이력 조회 경로만 대상으로 한다.
