package com.artinus.subscription.application.port.out;

import java.util.Optional;

import com.artinus.subscription.application.result.CompletedIdempotency;

public interface IdempotencyPort {

	Optional<CompletedIdempotency> findCompleted(String phoneNumber, String idempotencyKey);

	void saveCompleted(CompletedIdempotency completed);
}
