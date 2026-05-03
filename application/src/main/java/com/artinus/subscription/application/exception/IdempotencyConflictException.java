package com.artinus.subscription.application.exception;

public class IdempotencyConflictException extends ApplicationException {

	public IdempotencyConflictException() {
		super("같은 멱등성 키가 다른 요청 본문으로 이미 사용되었습니다.");
	}
}
