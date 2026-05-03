package com.artinus.subscription.application.idempotency;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

import com.artinus.subscription.application.exception.IdempotencyConflictException;
import com.artinus.subscription.application.exception.RequestHashCreationException;
import com.artinus.subscription.application.port.out.IdempotencyPort;
import com.artinus.subscription.application.result.CompletedIdempotency;
import com.artinus.subscription.application.result.SubscriptionResult;
import com.artinus.subscription.domain.subscription.SubscriptionActionType;
import com.artinus.subscription.domain.subscription.SubscriptionStatus;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class IdempotencyProcessor {

	private final IdempotencyPort idempotencyPort;

	public IdempotencyContext createContext(SubscriptionActionType actionType, String phoneNumber, Long channelId,
		SubscriptionStatus targetStatus, String idempotencyKey) {
		return new IdempotencyContext(phoneNumber, idempotencyKey,
			requestHash(actionType, phoneNumber, channelId, targetStatus));
	}

	public Optional<SubscriptionResult> findCompletedResult(IdempotencyContext idempotency) {
		return idempotencyPort.findCompleted(idempotency.phoneNumber(), idempotency.idempotencyKey())
			.map(completed -> {
				if (!completed.requestHash().equals(idempotency.requestHash())) {
					throw new IdempotencyConflictException();
				}
				return completed.response();
			});
	}

	public void saveCompletedResult(IdempotencyContext idempotency, SubscriptionResult result) {
		idempotencyPort.saveCompleted(new CompletedIdempotency(idempotency.phoneNumber(),
			idempotency.idempotencyKey(), idempotency.requestHash(), result));
	}

	private String requestHash(SubscriptionActionType actionType, String phoneNumber, Long channelId,
		SubscriptionStatus targetStatus) {
		String source = actionType + "|" + phoneNumber + "|" + channelId + "|" + targetStatus;
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(source.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException exception) {
			throw new RequestHashCreationException(exception);
		}
	}
}
