package com.artinus.subscription.application.port.in;

import com.artinus.subscription.application.result.SubscriptionHistoryResult;

public interface SubscriptionHistoryQueryUseCase {

	SubscriptionHistoryResult findByPhoneNumber(String phoneNumber);
}
