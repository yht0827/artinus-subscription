package com.artinus.subscription.infrastructure.persistence;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.artinus.subscription.domain.subscription.SubscriptionActionType;
import com.artinus.subscription.domain.subscription.SubscriptionStatus;
import com.artinus.subscription.infrastructure.persistence.entity.JpaChannelEntity;
import com.artinus.subscription.infrastructure.persistence.entity.JpaIdempotencyKeyEntity;
import com.artinus.subscription.infrastructure.persistence.entity.JpaMemberEntity;
import com.artinus.subscription.infrastructure.persistence.entity.JpaSubscriptionHistoryEntity;
import com.artinus.subscription.infrastructure.persistence.repository.ChannelRepository;
import com.artinus.subscription.infrastructure.persistence.repository.IdempotencyKeyRepository;
import com.artinus.subscription.infrastructure.persistence.repository.MemberRepository;
import com.artinus.subscription.infrastructure.persistence.repository.SubscriptionHistoryRepository;

@DataJpaTest
class SubscriptionPersistenceTest {

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private ChannelRepository channelRepository;

	@Autowired
	private SubscriptionHistoryRepository historyRepository;

	@Autowired
	private IdempotencyKeyRepository idempotencyKeyRepository;

	@Test
	void loadsSeededChannels() {
		List<JpaChannelEntity> channels = channelRepository.findAll();

		assertThat(channels).hasSize(6);
		assertThat(channels)
			.extracting(JpaChannelEntity::getName)
			.containsExactlyInAnyOrder("홈페이지", "모바일앱", "네이버", "SKT", "콜센터", "이메일");
	}

	@Test
	void savesAndFindsMemberByPhoneNumber() {
		JpaMemberEntity member = JpaMemberEntity.create("01012345678", SubscriptionStatus.NONE);
		memberRepository.save(member);

		assertThat(memberRepository.findByPhoneNumber("01012345678"))
			.hasValueSatisfying(found -> assertThat(found.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.NONE));
	}

	@Test
	void savesSubscriptionHistoryAndFindsByMemberIdOrderedByChangedAt() {
		JpaMemberEntity member = memberRepository.save(JpaMemberEntity.create("01012345678", SubscriptionStatus.NONE));
		JpaChannelEntity homepage = channelRepository.findByName("홈페이지").orElseThrow();
		JpaChannelEntity callCenter = channelRepository.findByName("콜센터").orElseThrow();

		historyRepository.save(JpaSubscriptionHistoryEntity.record(member, homepage, SubscriptionActionType.SUBSCRIBE,
			SubscriptionStatus.NONE, SubscriptionStatus.BASIC, LocalDateTime.of(2026, 1, 1, 10, 0)));
		historyRepository.save(JpaSubscriptionHistoryEntity.record(member, callCenter, SubscriptionActionType.CANCEL,
			SubscriptionStatus.BASIC, SubscriptionStatus.NONE, LocalDateTime.of(2026, 1, 2, 10, 0)));

		List<JpaSubscriptionHistoryEntity> histories = historyRepository.findByMemberIdOrderByChangedAtAsc(member.getId());

		assertThat(histories)
			.extracting(JpaSubscriptionHistoryEntity::getActionType)
			.containsExactly(SubscriptionActionType.SUBSCRIBE, SubscriptionActionType.CANCEL);
	}

	@Test
	void savesAndFindsIdempotencyKeyByPhoneNumberAndKey() {
		JpaIdempotencyKeyEntity idempotencyKey = JpaIdempotencyKeyEntity.completed(
			"01012345678",
			"request-key",
			"request-hash",
			"{\"subscriptionStatus\":\"BASIC\"}",
			200
		);

		idempotencyKeyRepository.save(idempotencyKey);

		assertThat(idempotencyKeyRepository.findByPhoneNumberAndIdempotencyKey("01012345678", "request-key"))
			.hasValueSatisfying(found -> assertThat(found.getRequestHash()).isEqualTo("request-hash"));
	}
}
