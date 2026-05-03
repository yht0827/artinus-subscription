# 시퀀스 다이어그램

> ARTINUS Subscription API 주요 런타임 흐름

## 목차

- [구독 신청 성공](#구독-신청-성공)
- [구독 해지 성공](#구독-해지-성공)
- [멱등성 재요청](#멱등성-재요청)
- [외부 승인 거절 및 장애](#외부-승인-거절-및-장애)
- [구독 이력 조회 및 LLM 요약](#구독-이력-조회-및-llm-요약)
- [동시성 충돌](#동시성-충돌)

---

## 구독 신청 성공

```mermaid
sequenceDiagram
    participant C as Client
    participant API as SubscriptionCommandController
    participant UC as SubscriptionCommandService
    participant IP as IdempotencyProcessor
    participant MP as MemberPort
    participant CP as ChannelPort
    participant EXT as CsrngExternalApprovalClient
    participant HP as SubscriptionHistoryPort
    participant DB as MySQL

    C->>API: POST /api/v1/subscriptions
    API->>UC: subscribe(command)
    UC->>IP: createContext(SUBSCRIBE, phone, channelId, targetStatus, key)
    IP->>DB: SELECT idempotency_keys

    alt 완료된 동일 요청 존재
        DB-->>IP: completed response
        IP-->>UC: SubscriptionResult
        UC-->>API: result
        API-->>C: 200 OK
    else 신규 요청
        UC->>MP: findByPhoneNumber(phone)
        MP->>DB: SELECT members

        alt 회원 없음
            UC->>MP: save(Member.NONE)
            MP->>DB: INSERT members
        end

        UC->>CP: getById(channelId)
        CP->>DB: SELECT channels
        UC->>UC: member.validateSubscribe(channel, targetStatus)
        UC->>EXT: approve()
        EXT->>EXT: retry + circuit breaker
        EXT-->>UC: approved=true
        UC->>UC: member.subscribe()
        UC->>MP: save(member)
        MP->>DB: UPDATE members
        UC->>HP: save(history)
        HP->>DB: INSERT subscription_histories
        UC->>IP: saveCompletedResult(result)
        IP->>DB: INSERT/UPDATE idempotency_keys
        UC-->>API: SubscriptionResult
        API-->>C: 200 OK
    end
```

핵심 포인트

- 멱등성 확인을 먼저 수행한다.
- 도메인 검증이 끝난 뒤 CSRNG 외부 승인 API를 호출한다.
- CSRNG 승인 이후 회원 상태, 이력, 멱등성 완료 결과를 하나의 트랜잭션 안에서 저장한다.

---

## 구독 해지 성공

```mermaid
sequenceDiagram
    participant C as Client
    participant API as SubscriptionCommandController
    participant UC as SubscriptionCommandService
    participant MP as MemberPort
    participant CP as ChannelPort
    participant EXT as CsrngExternalApprovalClient
    participant HP as SubscriptionHistoryPort
    participant DB as MySQL

    C->>API: POST /api/v1/subscriptions/cancel
    API->>UC: cancel(command)
    UC->>DB: SELECT idempotency_keys
    UC->>MP: findByPhoneNumber(phone)
    MP->>DB: SELECT members

    alt 회원 없음
        UC-->>API: MemberNotFoundException
        API-->>C: 404 NOT FOUND
    else 회원 존재
        UC->>CP: getById(channelId)
        CP->>DB: SELECT channels
        UC->>UC: member.validateCancel(channel, targetStatus)
        UC->>EXT: approve()
        EXT-->>UC: approved=true
        UC->>UC: member.cancel()
        UC->>MP: save(member)
        MP->>DB: UPDATE members
        UC->>HP: save(history)
        HP->>DB: INSERT subscription_histories
        UC->>DB: UPSERT completed idempotency result
        UC-->>API: SubscriptionResult
        API-->>C: 200 OK
    end
```

핵심 포인트

- 해지는 기존 회원만 가능하다.
- 해지 가능 채널과 상태 전이 규칙을 모두 만족해야 한다.
- 콜센터, 이메일처럼 해지 전용 채널에서도 해지 처리가 가능하다.

---

## 멱등성 재요청

```mermaid
sequenceDiagram
    participant C as Client
    participant API as SubscriptionCommandController
    participant UC as SubscriptionCommandService
    participant IP as IdempotencyProcessor
    participant DB as MySQL

    C->>API: POST 요청 + Idempotency-Key
    API->>UC: subscribe/cancel(command)
    UC->>IP: createContext(action, phone, channelId, targetStatus, key)
    IP->>DB: SELECT idempotency_keys WHERE phone_number + idempotency_key

    alt 같은 key + 같은 request_hash + 완료 응답 존재
        DB-->>IP: response_body, status_code
        IP-->>UC: CompletedIdempotency
        UC-->>API: saved SubscriptionResult
        API-->>C: 200 OK
    else 같은 key + 다른 request_hash
        IP-->>UC: IdempotencyConflictException
        API-->>C: 400 BAD REQUEST
    else 완료 응답 없음
        UC->>UC: 신규 요청 처리
    end
```

핵심 포인트

- 멱등성 기준은 `phone_number + idempotency_key`다.
- 요청 본문이 달라지면 같은 키라도 충돌로 처리한다.
- 외부 API 거절/장애처럼 성공하지 못한 요청은 완료 결과로 저장하지 않는다.

---

## 외부 승인 거절 및 장애

```mermaid
sequenceDiagram
    participant UC as SubscriptionCommandService
    participant EXT as CsrngExternalApprovalClient
    participant R as ExternalApiResilience
    participant CSRNG as CSRNG API
    participant API as GlobalExceptionHandler
    participant C as Client

    UC->>EXT: approve()
    EXT->>R: execute(csrng request)
    R->>CSRNG: GET /csrng.php?min=0&max=1

    alt random = 1
        CSRNG-->>R: approved response
        R-->>EXT: CsrngResponse[]
        EXT-->>UC: true
    else random = 0 or empty
        CSRNG-->>R: rejected response
        R-->>EXT: CsrngResponse[]
        EXT-->>UC: false
        UC-->>API: ExternalApprovalRejectedException
        API-->>C: 502 BAD GATEWAY
    else timeout / 5xx / network error
        R->>R: retry
        R->>R: circuit breaker state update
        R-->>EXT: RestClientException
        API-->>C: 500 or 502 mapped error
    end
```

핵심 포인트

- `random=0`은 비즈니스 거절이므로 성공으로 보정하지 않는다.
- timeout, 5xx, 네트워크 오류는 retry와 circuit breaker 대상이다.
- 외부 승인 실패 시 DB 상태 변경과 이력 저장은 진행하지 않는다.

---

## 구독 이력 조회 및 LLM 요약

```mermaid
sequenceDiagram
    participant C as Client
    participant API as SubscriptionCommandController
    participant QS as SubscriptionHistoryQueryService
    participant HP as SubscriptionHistoryPort
    participant SUM as HistorySummaryPort
    participant LLM as OpenAiHistorySummaryClient
    participant FB as FallbackHistorySummaryService
    participant DB as MySQL
    participant OpenAI as OpenAI Responses API

    C->>API: GET /api/v1/subscriptions/histories
    API->>QS: findByPhoneNumber(phoneNumber)
    QS->>QS: PhoneNumber 정규화
    QS->>HP: findByPhoneNumber(normalized)
    HP->>DB: SELECT subscription_histories
    DB-->>HP: histories
    HP-->>QS: histories

    alt LLM enabled + API key exists
        QS->>SUM: summarize(histories)
        SUM->>LLM: summarize(histories)
        LLM->>OpenAI: POST /v1/responses
        alt OpenAI success
            OpenAI-->>LLM: output_text
            LLM-->>QS: summary
        else OpenAI failure or blank response
            LLM->>FB: summarize(histories)
            FB-->>LLM: fallback summary
            LLM-->>QS: fallback summary
        end
    else LLM disabled
        QS->>FB: summarize(histories)
        FB-->>QS: fallback summary
    end

    QS-->>API: history + summary
    API-->>C: 200 OK
```

핵심 포인트

- LLM이 DB를 직접 조회하지 않는다.
- DB에서 조회한 이력 목록을 application 계층이 summary port로 전달한다.
- LLM 실패는 구독 이력 조회 API 실패로 전파하지 않는다.

---

## 동시성 충돌

```mermaid
sequenceDiagram
    participant C1 as Client A
    participant C2 as Client B
    participant API as SubscriptionCommandController
    participant UC as SubscriptionCommandService
    participant DB as MySQL members(version)
    participant EH as GlobalExceptionHandler

    C1->>API: 구독 상태 변경 요청
    C2->>API: 같은 회원 상태 변경 요청
    API->>UC: command A
    API->>UC: command B
    UC->>DB: SELECT member version=1
    UC->>DB: SELECT member version=1
    UC->>DB: UPDATE member SET version=2 WHERE version=1
    DB-->>UC: success
    UC->>DB: UPDATE member SET version=2 WHERE version=1
    DB-->>UC: optimistic locking failure
    UC-->>EH: OptimisticLockingFailureException
    EH-->>C2: 409 CONFLICT
```

핵심 포인트

- `members.version`은 JPA `@Version`으로 관리한다.
- 동시에 같은 회원 상태를 바꾸면 한 요청만 성공하고 나머지는 409로 응답한다.
- 클라이언트는 최신 상태를 다시 조회한 뒤 재시도해야 한다.
