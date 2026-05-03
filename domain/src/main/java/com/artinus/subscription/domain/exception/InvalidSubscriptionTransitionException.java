package com.artinus.subscription.domain.exception;

import com.artinus.subscription.domain.subscription.SubscriptionActionType;
import com.artinus.subscription.domain.subscription.SubscriptionStatus;

public class InvalidSubscriptionTransitionException extends DomainException {

	public InvalidSubscriptionTransitionException(SubscriptionActionType actionType, SubscriptionStatus current,
		SubscriptionStatus target) {
		super("허용되지 않는 구독 상태 변경입니다. actionType=%s, current=%s, target=%s"
			.formatted(actionType, current, target));
	}
}
