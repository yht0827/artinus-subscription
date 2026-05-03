package com.artinus.subscription.application.result;

import com.artinus.subscription.domain.subscription.SubscriptionStatus;

public record SubscriptionResult(String phoneNumber, SubscriptionStatus subscriptionStatus) {
}
