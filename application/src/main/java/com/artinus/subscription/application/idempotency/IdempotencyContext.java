package com.artinus.subscription.application.idempotency;

public record IdempotencyContext(String phoneNumber, String idempotencyKey, String requestHash) {
}
