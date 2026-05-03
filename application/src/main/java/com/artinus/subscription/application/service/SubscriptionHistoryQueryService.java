package com.artinus.subscription.application.service;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import com.artinus.subscription.application.port.in.SubscriptionHistoryQueryUseCase;
import com.artinus.subscription.application.port.out.HistorySummaryPort;
import com.artinus.subscription.application.port.out.SubscriptionHistoryPort;
import com.artinus.subscription.application.result.SubscriptionHistoryResult;
import com.artinus.subscription.domain.history.SubscriptionHistory;
import com.artinus.subscription.domain.member.PhoneNumber;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SubscriptionHistoryQueryService implements SubscriptionHistoryQueryUseCase {

	private final SubscriptionHistoryPort historyPort;
	private final HistorySummaryPort summaryPort;

	@Override
	@Transactional(readOnly = true)
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
