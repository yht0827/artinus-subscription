package com.artinus.subscription.application.port.out;

import com.artinus.subscription.domain.channel.Channel;

public interface ChannelPort {

	Channel getById(Long channelId);
}
