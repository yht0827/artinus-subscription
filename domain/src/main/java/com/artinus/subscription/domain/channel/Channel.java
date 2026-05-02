package com.artinus.subscription.domain.channel;

public record Channel(Long id, String name, boolean subscribeEnabled, boolean cancelEnabled) {

	public boolean supportsSubscribe() {
		return subscribeEnabled;
	}

	public boolean supportsCancel() {
		return cancelEnabled;
	}
}
