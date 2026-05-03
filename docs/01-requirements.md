# 요구사항 정의서

> ARTINUS Subscription API 요구사항 정의 (현재 코드 기준)

## 목차

- [유비쿼터스 언어](#유비쿼터스-언어)
- [시스템 개요](#시스템-개요)
- [API 전체 요약](#api-전체-요약)
- [구독 신청](#구독-신청)
- [구독 해지](#구독-해지)
- [구독 이력 조회](#구독-이력-조회)
- [상태 전이 규칙](#상태-전이-규칙)
- [외부 API 및 LLM 정책](#외부-api-및-llm-정책)
- [비기능 요구사항](#비기능-요구사항)

---

## 유비쿼터스 언어

| 한글 용어 | 영문 용어 | 설명 |
|---|---|---|
| 회원 | Member | 휴대폰번호로 식별되는 구독 주체 |
| 휴대폰번호 | PhoneNumber | 회원 식별 값 객체, 숫자만 저장 |
| 구독 상태 | SubscriptionStatus | `NONE`, `BASIC`, `PREMIUM` |
| 채널 | Channel | 구독/해지 요청이 유입되는 접점 |
| 구독 이력 | SubscriptionHistory | 구독 상태 변경 기록 |
| 멱등성 키 | Idempotency Key | 동일 요청 중복 처리를 막는 요청 식별자 |
| 외부 승인 | External Approval | CSRNG API 응답으로 구독/해지 처리 가능 여부 판단 |
| LLM 요약 | History Summary | 구독 이력을 자연어 문장으로 요약한 결과 |
| Fallback 요약 | Fallback Summary | LLM 장애나 비활성화 시 코드 기반으로 생성하는 요약 |

---

## 시스템 개요

### 처리 구조

```text
Client
  -> SubscriptionCommandController
  -> SubscriptionCommandService
  -> IdempotencyProcessor
  -> Member / Channel 도메인 검증
  -> CSRNG 외부 승인 API
  -> Member 상태 저장
  -> SubscriptionHistory 저장
  -> Idempotency 결과 저장
```

구독 이력 조회는 아래 흐름으로 동작한다.

```text
Client
  -> SubscriptionCommandController
  -> SubscriptionHistoryQueryService
  -> SubscriptionHistoryPort로 DB 조회
  -> HistorySummaryPort로 요약 생성
     - LLM_ENABLED=true + API Key 존재: OpenAI 호출
     - 그 외 또는 실패: FallbackHistorySummaryService
```

### 핵심 패턴

| 패턴 | 설명 |
|---|---|
| Hexagonal Architecture | `api -> application(port) -> domain`, `infrastructure(adapter)` 분리 |
| Idempotency Key | `(phone_number, idempotency_key)` 유니크 제약으로 중복 요청 방지 |
| Optimistic Lock | `members.version`으로 동시 구독 상태 변경 충돌 감지 |
| External API Resilience | CSRNG/OpenAI 호출에 timeout, retry, circuit breaker 적용 |
| LLM Fallback | LLM 실패 시 이력 조회 API는 실패시키지 않고 fallback 요약 반환 |
| Testcontainers | DB 통합 테스트를 MySQL Testcontainers로 검증 |

### 모듈 구성

| 모듈 | 책임 |
|---|---|
| `domain` | 값 객체, 도메인 모델, 상태 전이 정책 |
| `application` | 유스케이스, 포트, 멱등성 처리, fallback 요약 |
| `infrastructure` | JPA, Flyway, CSRNG client, OpenAI client, resilience |
| `api` | Controller, DTO, Swagger, 전역 예외 응답 |
| `app` | Spring Boot 실행 진입점, 설정 조립 |

---

## API 전체 요약

| 기능 | METHOD | URI | 주요 헤더 |
|---|---|---|---|
| 구독 신청 | POST | `/api/v1/subscriptions` | `Idempotency-Key` |
| 구독 해지 | POST | `/api/v1/subscriptions/cancel` | `Idempotency-Key` |
| 구독 이력 조회 | GET | `/api/v1/subscriptions/histories` | - |
| OpenAPI JSON | GET | `/v3/api-docs` | - |
| Swagger UI | GET | `/swagger-ui.html` | - |
| Health Check | GET | `/actuator/health` | - |

---

## 구독 신청

| METHOD | URI | 설명 |
|---|---|---|
| POST | `/api/v1/subscriptions` | 회원의 구독 상태를 `BASIC` 또는 `PREMIUM`으로 변경 |

### 기능 요구사항

- `Idempotency-Key` 헤더는 필수다.
- 휴대폰번호, 채널 ID, 변경할 구독 상태를 입력받는다.
- 최초 회원은 `NONE` 상태로 생성한 뒤 구독 상태 전이를 수행한다.
- 채널이 구독 가능한 경우에만 처리한다.
- 도메인 상태 전이 규칙을 통과해야 한다.
- CSRNG 외부 승인 API의 `random=1` 응답일 때만 상태를 변경한다.
- 성공 시 회원 상태, 구독 이력, 멱등성 완료 결과를 저장한다.

### Request

```http
POST /api/v1/subscriptions
Idempotency-Key: subscribe-basic-001
Content-Type: application/json
```

```json
{
  "phoneNumber": "010-1234-5678",
  "channelId": 1,
  "targetStatus": "BASIC"
}
```

### Response

```json
{
  "phoneNumber": "01012345678",
  "subscriptionStatus": "BASIC"
}
```

### 실패 케이스

| 케이스 | HTTP 상태 | 메시지 방향 |
|---|---:|---|
| 멱등성 키 누락 | 400 | 헤더 필수 |
| 요청 본문 파싱 실패 | 400 | 요청 본문 형식 오류 |
| 필수값 누락 | 400 | validation 메시지 |
| 구독 불가 채널 | 400 | 도메인 예외 |
| 불가능한 상태 전이 | 400 | 도메인 예외 |
| 멱등성 키 충돌 | 400 | 동일 키 다른 요청 |
| 외부 승인 거절 | 502 | 외부 승인 API 거절 |
| 동시 수정 충돌 | 409 | 다시 조회 후 재시도 |

---

## 구독 해지

| METHOD | URI | 설명 |
|---|---|---|
| POST | `/api/v1/subscriptions/cancel` | 회원의 구독 상태를 `BASIC` 또는 `NONE`으로 변경 |

### 기능 요구사항

- `Idempotency-Key` 헤더는 필수다.
- 기존 회원이 존재해야 한다.
- 채널이 해지 가능한 경우에만 처리한다.
- 도메인 상태 전이 규칙을 통과해야 한다.
- CSRNG 외부 승인 API의 `random=1` 응답일 때만 상태를 변경한다.
- 성공 시 회원 상태, 구독 이력, 멱등성 완료 결과를 저장한다.

### Request

```http
POST /api/v1/subscriptions/cancel
Idempotency-Key: cancel-to-none-001
Content-Type: application/json
```

```json
{
  "phoneNumber": "010-1234-5678",
  "channelId": 5,
  "targetStatus": "NONE"
}
```

### Response

```json
{
  "phoneNumber": "01012345678",
  "subscriptionStatus": "NONE"
}
```

---

## 구독 이력 조회

| METHOD | URI | 설명 |
|---|---|---|
| GET | `/api/v1/subscriptions/histories?phoneNumber=010-1234-5678` | 휴대폰번호 기준 구독 변경 이력과 요약 조회 |

### 기능 요구사항

- 휴대폰번호를 정규화해 이력을 조회한다.
- 이력은 채널, 구독/해지 타입, 이전 상태, 이후 상태, 변경 시각을 포함한다.
- 이력 목록을 기반으로 요약 문장을 생성한다.
- LLM이 비활성화되었거나 실패하면 fallback 요약을 반환한다.

### Response

```json
{
  "history": [
    {
      "channelName": "홈페이지",
      "actionType": "SUBSCRIBE",
      "beforeStatus": "NONE",
      "afterStatus": "BASIC",
      "changedAt": "2026-05-03T22:14:35.598649"
    }
  ],
  "summary": "2026년 5월 3일 홈페이지를 통해 일반 구독으로 구독하였습니다."
}
```

---

## 상태 전이 규칙

### 구독 신청

| 현재 상태 | 변경 가능 상태 |
|---|---|
| `NONE` | `BASIC`, `PREMIUM` |
| `BASIC` | `PREMIUM` |
| `PREMIUM` | 변경 불가 |

### 구독 해지

| 현재 상태 | 변경 가능 상태 |
|---|---|
| `PREMIUM` | `BASIC`, `NONE` |
| `BASIC` | `NONE` |
| `NONE` | 변경 불가 |

### 채널 정책

| 채널 | 구독 | 해지 |
|---|---|---|
| 홈페이지 | O | O |
| 모바일앱 | O | O |
| 네이버 | O | X |
| SKT | O | X |
| 콜센터 | X | O |
| 이메일 | X | O |

---

## 외부 API 및 LLM 정책

### CSRNG 승인 API

- 호출 URL: `https://csrng.net/csrng/csrng.php?min=0&max=1`
- `random=1`: 승인
- `random=0`: 비즈니스 거절
- 네트워크 오류, timeout, 5xx 응답은 외부 API 장애로 본다.
- timeout, retry, circuit breaker를 적용한다.
- 비즈니스 거절은 성공으로 보정하지 않는다.

### OpenAI LLM 요약

- 기본값은 `LLM_ENABLED=false`다.
- 활성화 조건은 `LLM_ENABLED=true` 및 `OPENAI_API_KEY` 존재다.
- OpenAI Responses API를 호출해 구독 이력 요약을 생성한다.
- API key, 전화번호, 요청 본문은 로그에 남기지 않는다.
- 실패 시 fallback 요약을 반환한다.

---

## 비기능 요구사항

| 항목 | 요구사항 |
|---|---|
| 보안 | API Key는 `.env`로 로컬 관리하고 저장소에 커밋하지 않음 |
| 장애 대응 | CSRNG/OpenAI timeout, retry, circuit breaker 적용 |
| 멱등성 | 같은 `Idempotency-Key`와 같은 요청은 저장된 응답 재사용 |
| 동시성 | 회원 상태 변경은 JPA `@Version`으로 충돌 감지 |
| 테스트 | 단위/웹/API/영속성 테스트와 Testcontainers 기반 DB 통합 테스트 |
| CI | GitHub Actions에서 `./gradlew test` 실행 |
| 부하 검증 | k6 스모크 테스트로 health, 이력 조회 기본 응답 확인 |
| 운영 확장 | Redis, 비동기 전환, 아카이빙은 현재 구현 범위 밖의 확장 고려사항 |
