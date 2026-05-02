package com.artinus.subscription.domain.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PhoneNumberTest {

	@Test
	void normalizesHyphenatedPhoneNumber() {
		PhoneNumber phoneNumber = PhoneNumber.from("010-1234-5678");

		assertThat(phoneNumber.value()).isEqualTo("01012345678");
	}

	@Test
	void keepsAlreadyNormalizedPhoneNumber() {
		PhoneNumber phoneNumber = PhoneNumber.from("01012345678");

		assertThat(phoneNumber.value()).isEqualTo("01012345678");
	}

	@Test
	void rejectsBlankPhoneNumber() {
		assertThatThrownBy(() -> PhoneNumber.from(" "))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void rejectsPhoneNumberThatDoesNotStartWith010() {
		assertThatThrownBy(() -> PhoneNumber.from("01112345678"))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void rejectsShortPhoneNumber() {
		assertThatThrownBy(() -> PhoneNumber.from("0101234567"))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void rejectsLongPhoneNumber() {
		assertThatThrownBy(() -> PhoneNumber.from("010123456789"))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void rejectsNonNumericPhoneNumber() {
		assertThatThrownBy(() -> PhoneNumber.from("010-1234-abcd"))
			.isInstanceOf(IllegalArgumentException.class);
	}
}
