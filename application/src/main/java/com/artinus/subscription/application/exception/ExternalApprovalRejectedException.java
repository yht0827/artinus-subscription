package com.artinus.subscription.application.exception;

public class ExternalApprovalRejectedException extends ApplicationException {

	public ExternalApprovalRejectedException() {
		super("외부 승인 API가 요청을 거절했습니다.");
	}
}
