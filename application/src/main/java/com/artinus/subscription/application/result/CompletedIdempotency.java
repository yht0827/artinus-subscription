package com.artinus.subscription.application.result;

public record CompletedIdempotency(
	String phoneNumber,
	String idempotencyKey,
	String requestHash,
	SubscriptionResult response
) {
}
