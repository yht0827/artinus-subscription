package com.artinus.subscription.domain.member;

import com.artinus.subscription.domain.exception.InvalidPhoneNumberException;

public record PhoneNumber(String value) {

	private static final int LENGTH = 11;
	private static final String PREFIX = "010";

	public static PhoneNumber from(String rawPhoneNumber) {
		if (rawPhoneNumber == null || rawPhoneNumber.isBlank()) {
			throw new InvalidPhoneNumberException("휴대폰번호는 필수입니다.");
		}

		// 하이픈 입력도 같은 휴대폰번호로 취급하기 위해 숫자 문자열로 정규화한다.
		String normalized = rawPhoneNumber.replace("-", "");
		validate(normalized);
		return new PhoneNumber(normalized);
	}

	private static void validate(String normalized) {
		if (!normalized.matches("\\d+")) {
			throw new InvalidPhoneNumberException("휴대폰번호는 숫자만 입력할 수 있습니다.");
		}
		if (!normalized.startsWith(PREFIX)) {
			throw new InvalidPhoneNumberException("휴대폰번호는 010으로 시작해야 합니다.");
		}
		if (normalized.length() != LENGTH) {
			throw new InvalidPhoneNumberException("휴대폰번호는 11자리여야 합니다.");
		}
	}
}
