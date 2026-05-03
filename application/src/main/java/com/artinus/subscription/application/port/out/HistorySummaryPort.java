package com.artinus.subscription.application.port.out;

import java.util.List;

import com.artinus.subscription.domain.history.SubscriptionHistory;

public interface HistorySummaryPort {

	String summarize(List<SubscriptionHistory> histories);
}
