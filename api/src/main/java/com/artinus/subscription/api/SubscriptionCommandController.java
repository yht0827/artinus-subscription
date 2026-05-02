package com.artinus.subscription.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.artinus.subscription.application.SubscriptionCommandService;
import com.artinus.subscription.application.SubscriptionHistoryQueryService;
import com.artinus.subscription.application.command.CancelCommand;
import com.artinus.subscription.application.command.SubscribeCommand;
import com.artinus.subscription.application.result.SubscriptionHistoryResult;
import com.artinus.subscription.application.result.SubscriptionResult;
import com.artinus.subscription.domain.subscription.SubscriptionStatus;

@RestController
@RequestMapping("/api/v1/subscriptions")
public class SubscriptionCommandController {

	private final SubscriptionCommandService subscriptionCommandService;
	private final SubscriptionHistoryQueryService subscriptionHistoryQueryService;

	public SubscriptionCommandController(SubscriptionCommandService subscriptionCommandService,
		SubscriptionHistoryQueryService subscriptionHistoryQueryService) {
		this.subscriptionCommandService = subscriptionCommandService;
		this.subscriptionHistoryQueryService = subscriptionHistoryQueryService;
	}

	@PostMapping
	public ResponseEntity<SubscriptionResult> subscribe(@RequestHeader("Idempotency-Key") String idempotencyKey,
		@RequestBody SubscriptionCommandRequest request) {
		SubscriptionResult result = subscriptionCommandService.subscribe(new SubscribeCommand(
			request.phoneNumber(),
			request.channelId(),
			request.targetStatus(),
			idempotencyKey
		));
		return ResponseEntity.ok(result);
	}

	@PostMapping("/cancel")
	public ResponseEntity<SubscriptionResult> cancel(@RequestHeader("Idempotency-Key") String idempotencyKey,
		@RequestBody SubscriptionCommandRequest request) {
		SubscriptionResult result = subscriptionCommandService.cancel(new CancelCommand(
			request.phoneNumber(),
			request.channelId(),
			request.targetStatus(),
			idempotencyKey
		));
		return ResponseEntity.ok(result);
	}

	@GetMapping("/histories")
	public ResponseEntity<SubscriptionHistoryResult> findHistories(@RequestParam("phoneNumber") String phoneNumber) {
		return ResponseEntity.ok(subscriptionHistoryQueryService.findByPhoneNumber(phoneNumber));
	}

	public record SubscriptionCommandRequest(
		String phoneNumber,
		Long channelId,
		SubscriptionStatus targetStatus
	) {
	}
}
