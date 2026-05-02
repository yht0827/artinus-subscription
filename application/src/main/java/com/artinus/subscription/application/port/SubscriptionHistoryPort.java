package com.artinus.subscription.application.port;

import java.util.List;

import com.artinus.subscription.domain.history.SubscriptionHistory;

public interface SubscriptionHistoryPort {

	void save(SubscriptionHistory history);

	List<SubscriptionHistory> findByPhoneNumber(String phoneNumber);
}
