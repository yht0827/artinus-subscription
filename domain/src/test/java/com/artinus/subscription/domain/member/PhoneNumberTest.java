package com.artinus.subscription.domain.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.artinus.subscription.domain.exception.InvalidPhoneNumberException;

class PhoneNumberTest {

	@Test
	void normalizesHyphenatedPhoneNumber() {
		// when
		PhoneNumber phoneNumber = PhoneNumber.from("010-1234-5678");

		// then
		assertThat(phoneNumber.value()).isEqualTo("01012345678");
	}

	@Test
	void keepsAlreadyNormalizedPhoneNumber() {
		// when
		PhoneNumber phoneNumber = PhoneNumber.from("01012345678");

		// then
		assertThat(phoneNumber.value()).isEqualTo("01012345678");
	}

	@Test
	void rejectsBlankPhoneNumber() {
		// when & then
		assertThatThrownBy(() -> PhoneNumber.from(" "))
			.isInstanceOf(InvalidPhoneNumberException.class);
	}

	@Test
	void rejectsNullPhoneNumber() {
		// when & then
		assertThatThrownBy(() -> PhoneNumber.from(null))
			.isInstanceOf(InvalidPhoneNumberException.class);
	}

	@Test
	void rejectsPhoneNumberThatDoesNotStartWith010() {
		// when & then
		assertThatThrownBy(() -> PhoneNumber.from("01112345678"))
			.isInstanceOf(InvalidPhoneNumberException.class);
	}

	@Test
	void rejectsShortPhoneNumber() {
		// when & then
		assertThatThrownBy(() -> PhoneNumber.from("0101234567"))
			.isInstanceOf(InvalidPhoneNumberException.class);
	}

	@Test
	void rejectsLongPhoneNumber() {
		// when & then
		assertThatThrownBy(() -> PhoneNumber.from("010123456789"))
			.isInstanceOf(InvalidPhoneNumberException.class);
	}

	@Test
	void rejectsNonNumericPhoneNumber() {
		// when & then
		assertThatThrownBy(() -> PhoneNumber.from("010-1234-abcd"))
			.isInstanceOf(InvalidPhoneNumberException.class);
	}
}
