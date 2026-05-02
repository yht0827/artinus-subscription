package com.artinus.subscription.application.command;

import com.artinus.subscription.domain.subscription.SubscriptionStatus;

public record SubscribeCommand(String phoneNumber, Long channelId, SubscriptionStatus targetStatus) {
}
