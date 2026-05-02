package com.artinus.subscription.application;

import java.util.List;

import com.artinus.subscription.application.port.SubscriptionHistoryPort;
import com.artinus.subscription.application.result.SubscriptionHistoryResult;
import com.artinus.subscription.domain.history.SubscriptionHistory;
import com.artinus.subscription.domain.member.PhoneNumber;

public class SubscriptionHistoryQueryService {

	private final SubscriptionHistoryPort historyPort;
	private final HistorySummaryPort summaryPort;

	public SubscriptionHistoryQueryService(SubscriptionHistoryPort historyPort, HistorySummaryPort summaryPort) {
		this.historyPort = historyPort;
		this.summaryPort = summaryPort;
	}

	public SubscriptionHistoryResult findByPhoneNumber(String phoneNumber) {
		String normalizedPhoneNumber = PhoneNumber.from(phoneNumber).value();
		List<SubscriptionHistory> histories = historyPort.findByPhoneNumber(normalizedPhoneNumber);
		List<SubscriptionHistoryResult.HistoryItem> historyItems = histories
			.stream()
			.map(this::toHistoryItem)
			.toList();
		return new SubscriptionHistoryResult(historyItems, summaryPort.summarize(histories));
	}

	private SubscriptionHistoryResult.HistoryItem toHistoryItem(SubscriptionHistory history) {
		return new SubscriptionHistoryResult.HistoryItem(
			history.channel().name(),
			history.actionType(),
			history.beforeStatus(),
			history.afterStatus(),
			history.changedAt()
		);
	}
}
