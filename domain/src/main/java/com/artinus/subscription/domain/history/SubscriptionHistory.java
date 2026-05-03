package com.artinus.subscription.domain.history;

import java.time.LocalDateTime;

import com.artinus.subscription.domain.channel.Channel;
import com.artinus.subscription.domain.member.Member;
import com.artinus.subscription.domain.subscription.SubscriptionActionType;
import com.artinus.subscription.domain.subscription.SubscriptionStatus;

public record SubscriptionHistory(
	Member member,
	Channel channel,
	SubscriptionActionType actionType,
	SubscriptionStatus beforeStatus,
	SubscriptionStatus afterStatus,
	LocalDateTime changedAt
) {

	public static SubscriptionHistory record(Member member, Channel channel, SubscriptionActionType actionType,
		SubscriptionStatus beforeStatus, SubscriptionStatus afterStatus, LocalDateTime changedAt) {
		return new SubscriptionHistory(member, channel, actionType, beforeStatus, afterStatus, changedAt);
	}
}
