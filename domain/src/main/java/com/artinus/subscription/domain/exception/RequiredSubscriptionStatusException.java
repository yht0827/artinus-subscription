package com.artinus.subscription.domain.exception;

public class RequiredSubscriptionStatusException extends DomainException {

	public RequiredSubscriptionStatusException() {
		super("구독 상태는 필수입니다.");
	}
}
