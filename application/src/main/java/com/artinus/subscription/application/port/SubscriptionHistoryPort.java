package com.artinus.subscription.application.port;

import com.artinus.subscription.domain.history.SubscriptionHistory;

public interface SubscriptionHistoryPort {

	void save(SubscriptionHistory history);
}
