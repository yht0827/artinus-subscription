package com.artinus.subscription.application;

import java.util.List;

import com.artinus.subscription.domain.history.SubscriptionHistory;

public interface HistorySummaryPort {

	String summarize(List<SubscriptionHistory> histories);
}
