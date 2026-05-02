package com.artinus.subscription.infrastructure.persistence.adapter;

import org.springframework.stereotype.Repository;

import com.artinus.subscription.application.port.ChannelPort;
import com.artinus.subscription.domain.channel.Channel;
import com.artinus.subscription.infrastructure.persistence.entity.JpaChannelEntity;
import com.artinus.subscription.infrastructure.persistence.repository.ChannelRepository;

@Repository
public class JpaChannelAdapter implements ChannelPort {

	private final ChannelRepository channelRepository;

	public JpaChannelAdapter(ChannelRepository channelRepository) {
		this.channelRepository = channelRepository;
	}

	@Override
	public Channel getById(Long channelId) {
		return channelRepository.findById(channelId)
			.map(this::toDomain)
			.orElse(null);
	}

	private Channel toDomain(JpaChannelEntity entity) {
		return new Channel(entity.getId(), entity.getName(), entity.supportsSubscribe(), entity.supportsCancel());
	}
}
