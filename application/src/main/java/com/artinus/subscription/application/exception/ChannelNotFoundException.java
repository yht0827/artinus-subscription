package com.artinus.subscription.application.exception;

public class ChannelNotFoundException extends ApplicationException {

	public ChannelNotFoundException(Long channelId) {
		super("채널을 찾을 수 없습니다. channelId=%s".formatted(channelId));
	}
}
