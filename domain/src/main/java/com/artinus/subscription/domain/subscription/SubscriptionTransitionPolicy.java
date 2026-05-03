package com.artinus.subscription.domain.subscription;

import com.artinus.subscription.domain.exception.InvalidSubscriptionTransitionException;

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
		// 구독은 NONE에서 시작하거나 BASIC에서 PREMIUM으로 올리는 경우만 허용한다.
		return (current == SubscriptionStatus.NONE && target == SubscriptionStatus.BASIC)
			|| (current == SubscriptionStatus.NONE && target == SubscriptionStatus.PREMIUM)
			|| (current == SubscriptionStatus.BASIC && target == SubscriptionStatus.PREMIUM);
	}

	private boolean isAllowedCancel(SubscriptionStatus current, SubscriptionStatus target) {
		// 해지는 현재 등급보다 낮은 상태로 내려가는 경우만 허용한다.
		return (current == SubscriptionStatus.PREMIUM && target == SubscriptionStatus.BASIC)
			|| (current == SubscriptionStatus.PREMIUM && target == SubscriptionStatus.NONE)
			|| (current == SubscriptionStatus.BASIC && target == SubscriptionStatus.NONE);
	}
}
