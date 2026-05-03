# ARTINUS Subscription

## 프로젝트 개요

휴대폰번호 기반 회원 구독 상태를 관리하는 Spring Boot 백엔드 API입니다.

구독 신청, 구독 해지, 구독 이력 조회를 제공하며 CSRNG 외부 승인, LLM 이력 요약, 멱등성, 동시성 제어, 외부 API 장애 대응을 포함합니다.

```text
Client
  -> Subscription API
  -> Idempotency 검증
  -> 도메인 상태 전이 검증
  -> CSRNG 외부 승인
  -> 상태 변경 + 이력 저장
  -> LLM/Fallback 이력 요약
```

## 핵심 기능

- 구독 신청: `NONE -> BASIC/PREMIUM`, `BASIC -> PREMIUM`
- 구독 해지: `PREMIUM -> BASIC/NONE`, `BASIC -> NONE`
- 채널별 구독/해지 가능 여부 검증
- `Idempotency-Key` 기반 중복 요청 방지
- CSRNG 외부 API 승인 결과에 따른 트랜잭션 처리
- 구독 이력 조회 및 OpenAI 기반 자연어 요약
- LLM 비활성화/장애 시 fallback 요약 반환
- 동시 구독 상태 변경 충돌 응답 처리

## 기술 스택

| 카테고리 | 기술 |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5.14 |
| Database | MySQL 8.4 |
| ORM / Migration | Spring Data JPA, Flyway |
| Architecture | Multi-module, Hexagonal Architecture |
| External API | CSRNG, OpenAI Responses API |
| Resilience | Resilience4j Retry / Circuit Breaker |
| Test | JUnit 5, Spring Boot Test, Testcontainers |
| Load Test | k6 |
| CI | GitHub Actions |
| Build | Gradle |

## 아키텍처

```text
api
  -> application (usecase / port)
  -> domain
  <- infrastructure (adapter)
app
  -> configuration / bootstrapping
```

| 모듈 | 책임 |
|---|---|
| `domain` | 회원, 채널, 구독 상태, 상태 전이 정책 |
| `application` | 유스케이스, port, 멱등성 처리, fallback 요약 |
| `infrastructure` | JPA, Flyway, CSRNG client, OpenAI client, resilience |
| `api` | Controller, DTO, Swagger, 전역 예외 응답 |
| `app` | Spring Boot 실행 진입점, Bean 조립 |

## 구현 범위

- 도메인: 휴대폰번호 값 객체, 구독 상태 전이, 채널 정책
- API: 구독 신청, 구독 해지, 구독 이력 조회, Swagger 문서
- 데이터: MySQL, Flyway, JPA, Testcontainers
- 안정성: 멱등성, 낙관적 락, timeout, retry, circuit breaker
- LLM: OpenAI 요약, fallback 요약, 로컬 `.env` 기반 API key 관리
- 검증: GitHub Actions CI, k6 스모크 부하 테스트, 수동 검증 문서

## API

| 기능 | METHOD | URI |
|---|---|---|
| 구독 신청 | POST | `/api/v1/subscriptions` |
| 구독 해지 | POST | `/api/v1/subscriptions/cancel` |
| 구독 이력 조회 | GET | `/api/v1/subscriptions/histories?phoneNumber={phoneNumber}` |
| Health Check | GET | `/actuator/health` |
| OpenAPI JSON | GET | `/v3/api-docs` |

Swagger UI: `http://localhost:8080/swagger-ui.html`

## 실행 방법

```bash
docker compose up -d mysql
./gradlew :app:bootRun
```

로컬 MySQL 기본 접속 정보:

- host: `localhost`
- port: `3306`
- database: `artinus_subscription`
- username: `root`
- password: `password`

LLM 요약은 기본 비활성화입니다. OpenAI 연동이 필요할 때만 `.env`를 생성하고 `LLM_ENABLED=true`로 실행합니다.

```bash
cp .env.example .env
set -a
source .env
set +a
./gradlew :app:bootRun
```

## 테스트 및 검증

전체 테스트:

```bash
./gradlew test
```

DB 통합 테스트는 MySQL Testcontainers를 사용하므로 Docker 또는 OrbStack이 실행 중이어야 합니다.

k6 스모크 부하 테스트:

```bash
BASE_URL=http://localhost:8080 PHONE_NUMBER=010-7777-2214 k6 run k6/subscription-smoke.js
```

구독/해지 명령 API는 CSRNG 외부 API와 DB 상태 변경이 포함되므로 k6 스모크 대상에서는 제외했습니다.

## CI

GitHub Actions에서 `main`, `dev` 브랜치 push 및 PR 시 `./gradlew test`를 실행합니다.

## 문서

| 문서 | 설명 |
|---|---|
| [요구사항 정의](docs/01-requirements.md) | 도메인 용어, API 요구사항, 상태 전이, 외부 API/LLM 정책 |
| [시퀀스 다이어그램](docs/02-sequence-diagrams.md) | 구독/해지/멱등성/LLM/동시성 주요 런타임 흐름 |
| [클래스 다이어그램](docs/03-class-diagrams.md) | 계층 구조와 핵심 클래스 책임 |
| [ERD](docs/04-erd.md) | 테이블 관계, 인덱스, 운영 체크포인트 |
| [ADR](docs/05-adr.md) | 주요 기술 선택과 설계 결정 이유 |
| [최종 수동 검증 결과](docs/verification/manual-test-2026-05-03.md) | 로컬 실행, API 검증, k6 스모크 결과 |

## 제출 방법

안내 받은 마감일 전까지 GitHub public repository URL을 아래 메일로 회신합니다.

- `recruit@artinus.dev`
