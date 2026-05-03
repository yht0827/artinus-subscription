package com.artinus.subscription.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.artinus.subscription.api.docs.SubscriptionApiDocs;
import com.artinus.subscription.api.request.SubscriptionCommandRequest;
import com.artinus.subscription.api.response.SubscriptionCommandResponse;
import com.artinus.subscription.api.response.SubscriptionHistoryResponse;
import com.artinus.subscription.application.port.in.SubscriptionCommandUseCase;
import com.artinus.subscription.application.port.in.SubscriptionHistoryQueryUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionCommandController implements SubscriptionApiDocs {

	private final SubscriptionCommandUseCase subscriptionCommandUseCase;
	private final SubscriptionHistoryQueryUseCase subscriptionHistoryQueryUseCase;

	@Override
	@PostMapping
	public ResponseEntity<SubscriptionCommandResponse> subscribe(
		@RequestHeader("Idempotency-Key") String idempotencyKey,
		@Valid @RequestBody SubscriptionCommandRequest request) {
		log.info("구독 신청 요청 수신: channelId={}, targetStatus={}", request.channelId(), request.targetStatus());
		return ResponseEntity.ok(SubscriptionCommandResponse.from(
			subscriptionCommandUseCase.subscribe(request.toSubscribeCommand(idempotencyKey))
		));
	}

	@Override
	@PostMapping("/cancel")
	public ResponseEntity<SubscriptionCommandResponse> cancel(
		@RequestHeader("Idempotency-Key") String idempotencyKey,
		@Valid @RequestBody SubscriptionCommandRequest request) {
		log.info("구독 해지 요청 수신: channelId={}, targetStatus={}", request.channelId(), request.targetStatus());
		return ResponseEntity.ok(SubscriptionCommandResponse.from(
			subscriptionCommandUseCase.cancel(request.toCancelCommand(idempotencyKey))
		));
	}

	@Override
	@GetMapping("/histories")
	public ResponseEntity<SubscriptionHistoryResponse> findHistories(
		@RequestParam("phoneNumber") String phoneNumber) {
		log.info("구독 이력 조회 요청 수신");
		return ResponseEntity.ok(SubscriptionHistoryResponse.from(
			subscriptionHistoryQueryUseCase.findByPhoneNumber(phoneNumber)
		));
	}
}
