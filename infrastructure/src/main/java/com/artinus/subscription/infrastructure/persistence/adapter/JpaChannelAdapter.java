package com.artinus.subscription.infrastructure.persistence.adapter;

import org.springframework.stereotype.Repository;

import com.artinus.subscription.application.port.out.ChannelPort;
import com.artinus.subscription.domain.channel.Channel;
import com.artinus.subscription.application.exception.ChannelNotFoundException;
import com.artinus.subscription.infrastructure.persistence.entity.JpaChannelEntity;
import com.artinus.subscription.infrastructure.persistence.repository.ChannelRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class JpaChannelAdapter implements ChannelPort {

	private final ChannelRepository channelRepository;

	@Override
	public Channel getById(Long channelId) {
		return channelRepository.findById(channelId)
			.map(JpaChannelEntity::toDomain)
			.orElseThrow(() -> new ChannelNotFoundException(channelId));
	}
}
