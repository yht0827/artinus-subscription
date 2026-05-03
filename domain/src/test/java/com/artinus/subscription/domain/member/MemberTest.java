package com.artinus.subscription.domain.member;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.artinus.subscription.domain.channel.Channel;
import com.artinus.subscription.domain.exception.InvalidSubscriptionTransitionException;
import com.artinus.subscription.domain.exception.RequiredSubscriptionStatusException;
import com.artinus.subscription.domain.exception.UnsupportedChannelActionException;
import com.artinus.subscription.domain.history.SubscriptionHistory;
import com.artinus.subscription.domain.subscription.SubscriptionActionType;
import com.artinus.subscription.domain.subscription.SubscriptionStatus;

class MemberTest {

	@Test
	void subscribesAndRecordsHistory() {
		// given
		Member member = Member.create("01012345678", SubscriptionStatus.NONE);
		Channel homepage = new Channel(1L, "홈페이지", true, true);
		LocalDateTime changedAt = LocalDateTime.of(2026, 1, 1, 10, 0);

		// when
		SubscriptionHistory history = member.subscribe(homepage, SubscriptionStatus.BASIC, changedAt);

		// then
		assertThat(member.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.BASIC);
		assertThat(history.actionType()).isEqualTo(SubscriptionActionType.SUBSCRIBE);
		assertThat(history.beforeStatus()).isEqualTo(SubscriptionStatus.NONE);
		assertThat(history.afterStatus()).isEqualTo(SubscriptionStatus.BASIC);
		assertThat(history.changedAt()).isEqualTo(changedAt);
	}

	@Test
	void cancelsAndRecordsHistory() {
		// given
		Member member = Member.create("01012345678", SubscriptionStatus.BASIC);
		Channel callCenter = new Channel(5L, "콜센터", false, true);
		LocalDateTime changedAt = LocalDateTime.of(2026, 1, 2, 10, 0);

		// when
		SubscriptionHistory history = member.cancel(callCenter, SubscriptionStatus.NONE, changedAt);

		// then
		assertThat(member.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.NONE);
		assertThat(history.actionType()).isEqualTo(SubscriptionActionType.CANCEL);
		assertThat(history.beforeStatus()).isEqualTo(SubscriptionStatus.BASIC);
		assertThat(history.afterStatus()).isEqualTo(SubscriptionStatus.NONE);
		assertThat(history.changedAt()).isEqualTo(changedAt);
	}

	@Test
	void rejectsSubscribeWhenChannelDoesNotSupportSubscribe() {
		// given
		Member member = Member.create("01012345678", SubscriptionStatus.NONE);
		Channel callCenter = new Channel(5L, "콜센터", false, true);

		// when & then
		assertThatThrownBy(() -> member.validateSubscribe(callCenter, SubscriptionStatus.BASIC))
			.isInstanceOf(UnsupportedChannelActionException.class);
	}

	@Test
	void rejectsCancelWhenChannelDoesNotSupportCancel() {
		// given
		Member member = Member.create("01012345678", SubscriptionStatus.BASIC);
		Channel email = new Channel(6L, "이메일", true, false);

		// when & then
		assertThatThrownBy(() -> member.validateCancel(email, SubscriptionStatus.NONE))
			.isInstanceOf(UnsupportedChannelActionException.class);
	}

	@Test
	void rejectsCancelWhenTransitionIsInvalid() {
		// given
		Member member = Member.create("01012345678", SubscriptionStatus.NONE);
		Channel callCenter = new Channel(5L, "콜센터", false, true);

		// when & then
		assertThatThrownBy(() -> member.validateCancel(callCenter, SubscriptionStatus.BASIC))
			.isInstanceOf(InvalidSubscriptionTransitionException.class);
	}

	@Test
	void rejectsCreateWhenSubscriptionStatusIsNull() {
		// when & then
		assertThatThrownBy(() -> Member.create("01012345678", null))
			.isInstanceOf(RequiredSubscriptionStatusException.class);
	}

	@Test
	void rejectsChangeStatusWhenSubscriptionStatusIsNull() {
		// given
		Member member = Member.create("01012345678", SubscriptionStatus.BASIC);

		// when & then
		assertThatThrownBy(() -> member.changeStatus(null))
			.isInstanceOf(RequiredSubscriptionStatusException.class);
	}
}
