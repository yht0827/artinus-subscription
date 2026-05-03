# 최종 수동 검증 결과

검증 시각: 2026-05-03 22:15 KST

## 환경
- Docker MySQL: `artinus-subscription-mysql`, healthy
- Application: `http://localhost:8080`
- LLM: `LLM_ENABLED=false`

## 검증 명령

```bash
docker compose up -d mysql
./gradlew test
./gradlew :app:bootRun
```

## API 검증 결과

- `GET /actuator/health`: 200, `{"status":"UP"}`
- `GET /v3/api-docs`: 200
- `POST /api/v1/subscriptions`: 200, `BASIC` 구독 신청 성공
- `GET /api/v1/subscriptions/histories`: 200, 구독 이력과 fallback 요약 반환
- `POST /api/v1/subscriptions/cancel`: 200, `NONE` 해지 성공

CSRNG 외부 승인 API는 랜덤 승인 정책이므로 일부 구독 신청 요청은 502 거절 응답을 반환했고, 승인 응답이 온 요청에서 구독 상태 변경이 정상 처리되는 것을 확인했습니다.

## k6 스모크 부하 테스트

```bash
BASE_URL=http://localhost:8080 PHONE_NUMBER=010-7777-2214 k6 run k6/subscription-smoke.js
```

결과:
- VUs: 5
- Duration: 30s
- HTTP requests: 300
- Failed requests: 0.00%
- Checks: 450 / 450 성공
- `http_req_duration` p95: 32.79ms
- Thresholds: 통과

## 비고

- 구독/해지 명령 API는 CSRNG 외부 API 호출과 DB 상태 변경이 포함되므로 k6 스모크 대상에서는 제외했습니다.
- k6 스모크는 health check와 이력 조회 경로의 기본 응답 안정성을 확인하는 용도입니다.
