package com.artinus.subscription.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.artinus.subscription.domain.channel.Channel;
import com.artinus.subscription.domain.history.SubscriptionHistory;
import com.artinus.subscription.domain.member.Member;
import com.artinus.subscription.domain.subscription.SubscriptionActionType;
import com.artinus.subscription.domain.subscription.SubscriptionStatus;

class FallbackHistorySummaryServiceTest {

	private final FallbackHistorySummaryService service = new FallbackHistorySummaryService();

	@Test
	void summarizesHistoriesInKorean() {
		Member member = Member.create("01012345678", SubscriptionStatus.NONE);
		List<SubscriptionHistory> histories = List.of(
			SubscriptionHistory.record(member, new Channel(1L, "홈페이지", true, true),
				SubscriptionActionType.SUBSCRIBE, SubscriptionStatus.NONE, SubscriptionStatus.BASIC,
				LocalDateTime.of(2026, 1, 1, 10, 0)),
			SubscriptionHistory.record(member, new Channel(5L, "콜센터", false, true),
				SubscriptionActionType.CANCEL, SubscriptionStatus.BASIC, SubscriptionStatus.NONE,
				LocalDateTime.of(2026, 2, 1, 10, 0))
		);

		String summary = service.summarize(histories);

		assertThat(summary).isEqualTo(
			"2026년 1월 1일 홈페이지를 통해 일반 구독으로 구독하였습니다. "
				+ "2026년 2월 1일 콜센터를 통해 구독 안함으로 구독 해지하였습니다."
		);
	}

	@Test
	void returnsEmptyMessageWhenHistoryDoesNotExist() {
		assertThat(service.summarize(List.of())).isEqualTo("구독 이력이 없습니다.");
	}
}
