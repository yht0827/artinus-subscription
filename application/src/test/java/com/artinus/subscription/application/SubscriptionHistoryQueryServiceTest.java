package com.artinus.subscription.application;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.artinus.subscription.application.port.out.HistorySummaryPort;
import com.artinus.subscription.application.port.out.SubscriptionHistoryPort;
import com.artinus.subscription.application.result.SubscriptionHistoryResult;
import com.artinus.subscription.application.service.SubscriptionHistoryQueryService;
import com.artinus.subscription.domain.channel.Channel;
import com.artinus.subscription.domain.history.SubscriptionHistory;
import com.artinus.subscription.domain.member.Member;
import com.artinus.subscription.domain.subscription.SubscriptionActionType;
import com.artinus.subscription.domain.subscription.SubscriptionStatus;

class SubscriptionHistoryQueryServiceTest {

	private final FakeSubscriptionHistoryPort historyPort = new FakeSubscriptionHistoryPort();
	private final FakeHistorySummaryPort summaryPort = new FakeHistorySummaryPort();
	private final SubscriptionHistoryQueryService service = new SubscriptionHistoryQueryService(historyPort,
		summaryPort);

	@Test
	void findsSubscriptionHistoriesByPhoneNumber() {
		// given
		Member member = Member.create("01012345678", SubscriptionStatus.BASIC);
		Channel homepage = new Channel(1L, "홈페이지", true, true);
		historyPort.histories.add(SubscriptionHistory.record(member, homepage, SubscriptionActionType.SUBSCRIBE,
			SubscriptionStatus.NONE, SubscriptionStatus.BASIC, LocalDateTime.of(2026, 1, 1, 10, 0)));

		// when
		SubscriptionHistoryResult result = service.findByPhoneNumber("010-1234-5678");

		// then
		assertThat(result.history()).hasSize(1);
		assertThat(result.history().getFirst().channelName()).isEqualTo("홈페이지");
		assertThat(result.history().getFirst().afterStatus()).isEqualTo(SubscriptionStatus.BASIC);
		assertThat(result.summary()).isEqualTo("fallback summary");
	}

	private static class FakeSubscriptionHistoryPort implements SubscriptionHistoryPort {

		private final List<SubscriptionHistory> histories = new ArrayList<>();

		@Override
		public void save(SubscriptionHistory history) {
			histories.add(history);
		}

		@Override
		public List<SubscriptionHistory> findByPhoneNumber(String phoneNumber) {
			return histories.stream()
				.filter(history -> history.member().getPhoneNumber().equals(phoneNumber))
				.toList();
		}
	}

	private static class FakeHistorySummaryPort implements HistorySummaryPort {

		@Override
		public String summarize(List<SubscriptionHistory> histories) {
			return "fallback summary";
		}
	}
}
