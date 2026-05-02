package com.artinus.subscription.domain.subscription;

public class SubscriptionTransitionPolicy {

	public void validateSubscribe(SubscriptionStatus current, SubscriptionStatus target) {
		if (isAllowedSubscribe(current, target)) {
			return;
		}
		throw new InvalidSubscriptionTransitionException(SubscriptionActionType.SUBSCRIBE, current, target);
	}

	public void validateCancel(SubscriptionStatus current, SubscriptionStatus target) {
		if (isAllowedCancel(current, target)) {
			return;
		}
		throw new InvalidSubscriptionTransitionException(SubscriptionActionType.CANCEL, current, target);
	}

	private boolean isAllowedSubscribe(SubscriptionStatus current, SubscriptionStatus target) {
		return (current == SubscriptionStatus.NONE && target == SubscriptionStatus.BASIC)
			|| (current == SubscriptionStatus.NONE && target == SubscriptionStatus.PREMIUM)
			|| (current == SubscriptionStatus.BASIC && target == SubscriptionStatus.PREMIUM);
	}

	private boolean isAllowedCancel(SubscriptionStatus current, SubscriptionStatus target) {
		return (current == SubscriptionStatus.PREMIUM && target == SubscriptionStatus.BASIC)
			|| (current == SubscriptionStatus.PREMIUM && target == SubscriptionStatus.NONE)
			|| (current == SubscriptionStatus.BASIC && target == SubscriptionStatus.NONE);
	}
}
