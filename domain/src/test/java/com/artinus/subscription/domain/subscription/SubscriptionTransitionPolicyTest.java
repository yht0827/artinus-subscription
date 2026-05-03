package com.artinus.subscription.domain.subscription;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.artinus.subscription.domain.exception.InvalidSubscriptionTransitionException;

class SubscriptionTransitionPolicyTest {

	private final SubscriptionTransitionPolicy policy = new SubscriptionTransitionPolicy();

	@Test
	void allowsSubscribeFromNoneToBasic() {
		// when & then
		assertThatCode(() -> policy.validateSubscribe(SubscriptionStatus.NONE, SubscriptionStatus.BASIC))
			.doesNotThrowAnyException();
	}

	@Test
	void allowsSubscribeFromNoneToPremium() {
		// when & then
		assertThatCode(() -> policy.validateSubscribe(SubscriptionStatus.NONE, SubscriptionStatus.PREMIUM))
			.doesNotThrowAnyException();
	}

	@Test
	void allowsSubscribeFromBasicToPremium() {
		// when & then
		assertThatCode(() -> policy.validateSubscribe(SubscriptionStatus.BASIC, SubscriptionStatus.PREMIUM))
			.doesNotThrowAnyException();
	}

	@Test
	void rejectsSubscribeFromPremium() {
		// when & then
		assertThatThrownBy(() -> policy.validateSubscribe(SubscriptionStatus.PREMIUM, SubscriptionStatus.BASIC))
			.isInstanceOf(InvalidSubscriptionTransitionException.class);
	}

	@Test
	void rejectsSameStatusSubscribe() {
		// when & then
		assertThatThrownBy(() -> policy.validateSubscribe(SubscriptionStatus.BASIC, SubscriptionStatus.BASIC))
			.isInstanceOf(InvalidSubscriptionTransitionException.class);
	}

	@Test
	void rejectsSubscribeToNone() {
		// when & then
		assertThatThrownBy(() -> policy.validateSubscribe(SubscriptionStatus.BASIC, SubscriptionStatus.NONE))
			.isInstanceOf(InvalidSubscriptionTransitionException.class);
	}

	@Test
	void allowsCancelFromPremiumToBasic() {
		// when & then
		assertThatCode(() -> policy.validateCancel(SubscriptionStatus.PREMIUM, SubscriptionStatus.BASIC))
			.doesNotThrowAnyException();
	}

	@Test
	void allowsCancelFromPremiumToNone() {
		// when & then
		assertThatCode(() -> policy.validateCancel(SubscriptionStatus.PREMIUM, SubscriptionStatus.NONE))
			.doesNotThrowAnyException();
	}

	@Test
	void allowsCancelFromBasicToNone() {
		// when & then
		assertThatCode(() -> policy.validateCancel(SubscriptionStatus.BASIC, SubscriptionStatus.NONE))
			.doesNotThrowAnyException();
	}

	@Test
	void rejectsCancelFromNone() {
		// when & then
		assertThatThrownBy(() -> policy.validateCancel(SubscriptionStatus.NONE, SubscriptionStatus.BASIC))
			.isInstanceOf(InvalidSubscriptionTransitionException.class);
	}

	@Test
	void rejectsSameStatusCancel() {
		// when & then
		assertThatThrownBy(() -> policy.validateCancel(SubscriptionStatus.PREMIUM, SubscriptionStatus.PREMIUM))
			.isInstanceOf(InvalidSubscriptionTransitionException.class);
	}

	@Test
	void rejectsCancelToPremiumFromBasic() {
		// when & then
		assertThatThrownBy(() -> policy.validateCancel(SubscriptionStatus.BASIC, SubscriptionStatus.PREMIUM))
			.isInstanceOf(InvalidSubscriptionTransitionException.class);
	}
}
