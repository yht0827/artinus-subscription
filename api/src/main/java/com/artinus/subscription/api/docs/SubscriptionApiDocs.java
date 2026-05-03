package com.artinus.subscription.api.docs;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import com.artinus.subscription.api.request.SubscriptionCommandRequest;
import com.artinus.subscription.api.response.SubscriptionCommandResponse;
import com.artinus.subscription.api.response.SubscriptionHistoryResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "구독", description = "구독 신청, 구독 해지, 구독 이력 조회 API")
public interface SubscriptionApiDocs {

	@Operation(summary = "구독 신청", description = "구독 가능한 채널을 통해 회원의 구독 상태를 BASIC 또는 PREMIUM으로 변경합니다.")
	@ApiResponse(responseCode = "200", description = "구독 신청 완료")
	ResponseEntity<SubscriptionCommandResponse> subscribe(
		@Parameter(description = "멱등성 키", required = true)
		@RequestHeader("Idempotency-Key") String idempotencyKey,
		@RequestBody SubscriptionCommandRequest request);

	@Operation(summary = "구독 해지", description = "해지 가능한 채널을 통해 회원의 구독 상태를 BASIC 또는 NONE으로 변경합니다.")
	@ApiResponse(responseCode = "200", description = "구독 해지 완료")
	ResponseEntity<SubscriptionCommandResponse> cancel(
		@Parameter(description = "멱등성 키", required = true)
		@RequestHeader("Idempotency-Key") String idempotencyKey,
		@RequestBody SubscriptionCommandRequest request);

	@Operation(summary = "구독 이력 조회", description = "휴대폰번호 기준으로 구독 변경 이력과 요약 문장을 조회합니다.")
	@ApiResponse(responseCode = "200", description = "구독 이력 조회 완료")
	ResponseEntity<SubscriptionHistoryResponse> findHistories(
		@Parameter(description = "회원 휴대폰번호", example = "010-1234-5678", required = true)
		@RequestParam("phoneNumber") String phoneNumber);
}
