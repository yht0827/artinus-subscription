package com.artinus.subscription.domain.exception;

import com.artinus.subscription.domain.subscription.SubscriptionActionType;

public class UnsupportedChannelActionException extends DomainException {

	public UnsupportedChannelActionException(SubscriptionActionType actionType) {
		super("채널이 해당 구독 작업을 지원하지 않습니다. actionType=%s".formatted(actionType));
	}
}
