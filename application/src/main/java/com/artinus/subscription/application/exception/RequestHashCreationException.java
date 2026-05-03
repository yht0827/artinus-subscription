package com.artinus.subscription.application.exception;

public class RequestHashCreationException extends ApplicationException {

	public RequestHashCreationException(Throwable cause) {
		super("요청 해시를 생성할 수 없습니다.", cause);
	}
}
