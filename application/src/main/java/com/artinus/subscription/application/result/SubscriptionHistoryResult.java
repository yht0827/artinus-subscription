package com.artinus.subscription.application.result;

import java.time.LocalDateTime;
import java.util.List;

import com.artinus.subscription.domain.subscription.SubscriptionActionType;
import com.artinus.subscription.domain.subscription.SubscriptionStatus;

public record SubscriptionHistoryResult(List<HistoryItem> history, String summary) {

	public record HistoryItem(
		String channelName,
		SubscriptionActionType actionType,
		SubscriptionStatus beforeStatus,
		SubscriptionStatus afterStatus,
		LocalDateTime changedAt
	) {
	}
}
