package com.artinus.subscription.domain.subscription;

public class InvalidSubscriptionTransitionException extends RuntimeException {

	public InvalidSubscriptionTransitionException(SubscriptionActionType actionType, SubscriptionStatus current,
		SubscriptionStatus target) {
		super("Invalid subscription transition. actionType=%s, current=%s, target=%s"
			.formatted(actionType, current, target));
	}
}
