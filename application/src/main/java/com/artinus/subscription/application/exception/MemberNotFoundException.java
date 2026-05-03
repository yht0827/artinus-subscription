package com.artinus.subscription.application.exception;

public class MemberNotFoundException extends ApplicationException {

	public MemberNotFoundException() {
		super("회원을 찾을 수 없습니다.");
	}
}
