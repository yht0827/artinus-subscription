package com.artinus.subscription.infrastructure.persistence.adapter;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.artinus.subscription.application.port.out.SubscriptionHistoryPort;
import com.artinus.subscription.domain.history.SubscriptionHistory;
import com.artinus.subscription.infrastructure.exception.PersistenceReferenceNotFoundException;
import com.artinus.subscription.infrastructure.persistence.entity.JpaChannelEntity;
import com.artinus.subscription.infrastructure.persistence.entity.JpaMemberEntity;
import com.artinus.subscription.infrastructure.persistence.entity.JpaSubscriptionHistoryEntity;
import com.artinus.subscription.infrastructure.persistence.repository.ChannelRepository;
import com.artinus.subscription.infrastructure.persistence.repository.MemberRepository;
import com.artinus.subscription.infrastructure.persistence.repository.SubscriptionHistoryRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class JpaSubscriptionHistoryAdapter implements SubscriptionHistoryPort {

	private final MemberRepository memberRepository;
	private final ChannelRepository channelRepository;
	private final SubscriptionHistoryRepository historyRepository;

	@Override
	public void save(SubscriptionHistory history) {
		JpaMemberEntity member = memberRepository.findByPhoneNumber(history.member().getPhoneNumber())
			.orElseThrow(() -> new PersistenceReferenceNotFoundException("구독 이력을 저장할 회원을 찾을 수 없습니다."));
		JpaChannelEntity channel = channelRepository.findById(history.channel().id())
			.orElseThrow(() -> new PersistenceReferenceNotFoundException("구독 이력을 저장할 채널을 찾을 수 없습니다."));
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
			entity.getChannel().toDomain(),
			entity.getActionType(),
			entity.getBeforeStatus(),
			entity.getAfterStatus(),
			entity.getChangedAt()
		);
	}
}
