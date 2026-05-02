package com.artinus.subscription.domain.subscription;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SubscriptionTransitionPolicyTest {

	private final SubscriptionTransitionPolicy policy = new SubscriptionTransitionPolicy();

	@Test
	void allowsSubscribeFromNoneToBasic() {
		assertThatCode(() -> policy.validateSubscribe(SubscriptionStatus.NONE, SubscriptionStatus.BASIC))
			.doesNotThrowAnyException();
	}

	@Test
	void allowsSubscribeFromNoneToPremium() {
		assertThatCode(() -> policy.validateSubscribe(SubscriptionStatus.NONE, SubscriptionStatus.PREMIUM))
			.doesNotThrowAnyException();
	}

	@Test
	void allowsSubscribeFromBasicToPremium() {
		assertThatCode(() -> policy.validateSubscribe(SubscriptionStatus.BASIC, SubscriptionStatus.PREMIUM))
			.doesNotThrowAnyException();
	}

	@Test
	void rejectsSubscribeFromPremium() {
		assertThatThrownBy(() -> policy.validateSubscribe(SubscriptionStatus.PREMIUM, SubscriptionStatus.BASIC))
			.isInstanceOf(InvalidSubscriptionTransitionException.class);
	}

	@Test
	void rejectsSameStatusSubscribe() {
		assertThatThrownBy(() -> policy.validateSubscribe(SubscriptionStatus.BASIC, SubscriptionStatus.BASIC))
			.isInstanceOf(InvalidSubscriptionTransitionException.class);
	}

	@Test
	void rejectsSubscribeToNone() {
		assertThatThrownBy(() -> policy.validateSubscribe(SubscriptionStatus.BASIC, SubscriptionStatus.NONE))
			.isInstanceOf(InvalidSubscriptionTransitionException.class);
	}

	@Test
	void allowsCancelFromPremiumToBasic() {
		assertThatCode(() -> policy.validateCancel(SubscriptionStatus.PREMIUM, SubscriptionStatus.BASIC))
			.doesNotThrowAnyException();
	}

	@Test
	void allowsCancelFromPremiumToNone() {
		assertThatCode(() -> policy.validateCancel(SubscriptionStatus.PREMIUM, SubscriptionStatus.NONE))
			.doesNotThrowAnyException();
	}

	@Test
	void allowsCancelFromBasicToNone() {
		assertThatCode(() -> policy.validateCancel(SubscriptionStatus.BASIC, SubscriptionStatus.NONE))
			.doesNotThrowAnyException();
	}

	@Test
	void rejectsCancelFromNone() {
		assertThatThrownBy(() -> policy.validateCancel(SubscriptionStatus.NONE, SubscriptionStatus.BASIC))
			.isInstanceOf(InvalidSubscriptionTransitionException.class);
	}

	@Test
	void rejectsSameStatusCancel() {
		assertThatThrownBy(() -> policy.validateCancel(SubscriptionStatus.PREMIUM, SubscriptionStatus.PREMIUM))
			.isInstanceOf(InvalidSubscriptionTransitionException.class);
	}

	@Test
	void rejectsCancelToPremiumFromBasic() {
		assertThatThrownBy(() -> policy.validateCancel(SubscriptionStatus.BASIC, SubscriptionStatus.PREMIUM))
			.isInstanceOf(InvalidSubscriptionTransitionException.class);
	}
}
