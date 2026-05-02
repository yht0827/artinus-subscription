package com.artinus.subscription.domain.member;

import com.artinus.subscription.domain.subscription.SubscriptionStatus;

public class Member {

	private final String phoneNumber;
	private SubscriptionStatus subscriptionStatus;

	private Member(String phoneNumber, SubscriptionStatus subscriptionStatus) {
		this.phoneNumber = PhoneNumber.from(phoneNumber).value();
		this.subscriptionStatus = subscriptionStatus;
	}

	public static Member create(String phoneNumber, SubscriptionStatus subscriptionStatus) {
		if (subscriptionStatus == null) {
			throw new IllegalArgumentException("Subscription status is required.");
		}
		return new Member(phoneNumber, subscriptionStatus);
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public SubscriptionStatus getSubscriptionStatus() {
		return subscriptionStatus;
	}

	public void changeStatus(SubscriptionStatus subscriptionStatus) {
		if (subscriptionStatus == null) {
			throw new IllegalArgumentException("Subscription status is required.");
		}
		this.subscriptionStatus = subscriptionStatus;
	}
}
