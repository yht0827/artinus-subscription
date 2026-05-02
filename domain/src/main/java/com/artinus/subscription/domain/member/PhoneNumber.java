package com.artinus.subscription.domain.member;

public record PhoneNumber(String value) {

	private static final int LENGTH = 11;
	private static final String PREFIX = "010";

	public static PhoneNumber from(String rawPhoneNumber) {
		if (rawPhoneNumber == null || rawPhoneNumber.isBlank()) {
			throw new IllegalArgumentException("Phone number is required.");
		}

		String normalized = rawPhoneNumber.replace("-", "");
		validate(normalized);
		return new PhoneNumber(normalized);
	}

	private static void validate(String normalized) {
		if (!normalized.matches("\\d+")) {
			throw new IllegalArgumentException("Phone number must contain only digits.");
		}
		if (!normalized.startsWith(PREFIX)) {
			throw new IllegalArgumentException("Phone number must start with 010.");
		}
		if (normalized.length() != LENGTH) {
			throw new IllegalArgumentException("Phone number must be 11 digits.");
		}
	}
}
