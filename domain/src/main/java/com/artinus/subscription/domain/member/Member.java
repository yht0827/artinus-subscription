package com.artinus.subscription.domain.member;

import java.time.LocalDateTime;

import com.artinus.subscription.domain.channel.Channel;
import com.artinus.subscription.domain.exception.RequiredSubscriptionStatusException;
import com.artinus.subscription.domain.exception.UnsupportedChannelActionException;
import com.artinus.subscription.domain.history.SubscriptionHistory;
import com.artinus.subscription.domain.subscription.SubscriptionActionType;
import com.artinus.subscription.domain.subscription.SubscriptionStatus;
import com.artinus.subscription.domain.subscription.SubscriptionTransitionPolicy;

import lombok.Getter;

@Getter
public class Member {

	private final String phoneNumber;
	private SubscriptionStatus subscriptionStatus;
	private final SubscriptionTransitionPolicy transitionPolicy = new SubscriptionTransitionPolicy();

	private Member(String phoneNumber, SubscriptionStatus subscriptionStatus) {
		this.phoneNumber = PhoneNumber.from(phoneNumber).value();
		this.subscriptionStatus = subscriptionStatus;
	}

	public static Member create(String phoneNumber, SubscriptionStatus subscriptionStatus) {
		if (subscriptionStatus == null) {
			throw new RequiredSubscriptionStatusException();
		}
		return new Member(phoneNumber, subscriptionStatus);
	}

	public void changeStatus(SubscriptionStatus subscriptionStatus) {
		if (subscriptionStatus == null) {
			throw new RequiredSubscriptionStatusException();
		}
		this.subscriptionStatus = subscriptionStatus;
	}

	public void validateSubscribe(Channel channel, SubscriptionStatus targetStatus) {
		// 채널 정책과 상태 전이 정책을 모두 통과해야 구독할 수 있다.
		if (!channel.supportsSubscribe()) {
			throw new UnsupportedChannelActionException(SubscriptionActionType.SUBSCRIBE);
		}
		transitionPolicy.validateSubscribe(subscriptionStatus, targetStatus);
	}

	public void validateCancel(Channel channel, SubscriptionStatus targetStatus) {
		// 해지는 해지 가능 채널에서만 허용한다.
		if (!channel.supportsCancel()) {
			throw new UnsupportedChannelActionException(SubscriptionActionType.CANCEL);
		}
		transitionPolicy.validateCancel(subscriptionStatus, targetStatus);
	}

	public SubscriptionHistory subscribe(Channel channel, SubscriptionStatus targetStatus, LocalDateTime changedAt) {
		validateSubscribe(channel, targetStatus);
		return changeStatusAndRecord(channel, SubscriptionActionType.SUBSCRIBE, targetStatus, changedAt);
	}

	public SubscriptionHistory cancel(Channel channel, SubscriptionStatus targetStatus, LocalDateTime changedAt) {
		validateCancel(channel, targetStatus);
		return changeStatusAndRecord(channel, SubscriptionActionType.CANCEL, targetStatus, changedAt);
	}

	private SubscriptionHistory changeStatusAndRecord(Channel channel, SubscriptionActionType actionType,
		SubscriptionStatus targetStatus, LocalDateTime changedAt) {
		// 상태 변경 전 값을 보관해 구독 이력의 before/after 상태를 남긴다.
		SubscriptionStatus beforeStatus = subscriptionStatus;
		changeStatus(targetStatus);
		return SubscriptionHistory.record(this, channel, actionType, beforeStatus, targetStatus, changedAt);
	}
}
