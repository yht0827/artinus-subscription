package com.artinus.subscription.application;

import java.util.List;

import com.artinus.subscription.application.port.SubscriptionHistoryPort;
import com.artinus.subscription.application.result.SubscriptionHistoryResult;
import com.artinus.subscription.domain.history.SubscriptionHistory;
import com.artinus.subscription.domain.member.PhoneNumber;

public class SubscriptionHistoryQueryService {

	private final SubscriptionHistoryPort historyPort;

	public SubscriptionHistoryQueryService(SubscriptionHistoryPort historyPort) {
		this.historyPort = historyPort;
	}

	public SubscriptionHistoryResult findByPhoneNumber(String phoneNumber) {
		String normalizedPhoneNumber = PhoneNumber.from(phoneNumber).value();
		List<SubscriptionHistoryResult.HistoryItem> historyItems = historyPort.findByPhoneNumber(normalizedPhoneNumber)
			.stream()
			.map(this::toHistoryItem)
			.toList();
		return new SubscriptionHistoryResult(historyItems, "");
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
