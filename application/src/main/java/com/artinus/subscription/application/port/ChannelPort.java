package com.artinus.subscription.application.port;

import com.artinus.subscription.domain.channel.Channel;

public interface ChannelPort {

	Channel getById(Long channelId);
}
