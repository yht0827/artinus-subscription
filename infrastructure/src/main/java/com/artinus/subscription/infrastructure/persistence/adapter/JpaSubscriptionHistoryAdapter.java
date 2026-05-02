package com.artinus.subscription.infrastructure.persistence.adapter;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.artinus.subscription.application.port.SubscriptionHistoryPort;
import com.artinus.subscription.domain.channel.Channel;
import com.artinus.subscription.domain.history.SubscriptionHistory;
import com.artinus.subscription.infrastructure.persistence.entity.JpaChannelEntity;
import com.artinus.subscription.infrastructure.persistence.entity.JpaMemberEntity;
import com.artinus.subscription.infrastructure.persistence.entity.JpaSubscriptionHistoryEntity;
import com.artinus.subscription.infrastructure.persistence.repository.ChannelRepository;
import com.artinus.subscription.infrastructure.persistence.repository.MemberRepository;
import com.artinus.subscription.infrastructure.persistence.repository.SubscriptionHistoryRepository;

@Repository
public class JpaSubscriptionHistoryAdapter implements SubscriptionHistoryPort {

	private final MemberRepository memberRepository;
	private final ChannelRepository channelRepository;
	private final SubscriptionHistoryRepository historyRepository;

	public JpaSubscriptionHistoryAdapter(MemberRepository memberRepository, ChannelRepository channelRepository,
		SubscriptionHistoryRepository historyRepository) {
		this.memberRepository = memberRepository;
		this.channelRepository = channelRepository;
		this.historyRepository = historyRepository;
	}

	@Override
	public void save(SubscriptionHistory history) {
		JpaMemberEntity member = memberRepository.findByPhoneNumber(history.member().getPhoneNumber())
			.orElseThrow(() -> new IllegalArgumentException("Member not found."));
		JpaChannelEntity channel = channelRepository.findById(history.channel().id())
			.orElseThrow(() -> new IllegalArgumentException("Channel not found."));
		historyRepository.save(JpaSubscriptionHistoryEntity.record(member, channel, history.actionType(),
			history.beforeStatus(), history.afterStatus(), history.changedAt()));
	}

	@Override
	public List<SubscriptionHistory> findByPhoneNumber(String phoneNumber) {
		return memberRepository.findByPhoneNumber(phoneNumber)
			.map(member -> historyRepository.findByMemberIdOrderByChangedAtAsc(member.getId()).stream()
				.map(this::toDomain)
				.toList())
			.orElseGet(List::of);
	}

	private SubscriptionHistory toDomain(JpaSubscriptionHistoryEntity entity) {
		return SubscriptionHistory.record(
			entity.getMember().toDomain(),
			toDomain(entity.getChannel()),
			entity.getActionType(),
			entity.getBeforeStatus(),
			entity.getAfterStatus(),
			entity.getChangedAt()
		);
	}

	private Channel toDomain(JpaChannelEntity entity) {
		return new Channel(entity.getId(), entity.getName(), entity.supportsSubscribe(), entity.supportsCancel());
	}
}
