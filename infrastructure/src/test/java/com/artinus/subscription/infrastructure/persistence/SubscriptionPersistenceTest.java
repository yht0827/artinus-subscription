package com.artinus.subscription.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.artinus.subscription.application.result.CompletedIdempotency;
import com.artinus.subscription.application.result.SubscriptionResult;
import com.artinus.subscription.domain.channel.Channel;
import com.artinus.subscription.domain.history.SubscriptionHistory;
import com.artinus.subscription.domain.member.Member;
import com.artinus.subscription.domain.subscription.SubscriptionActionType;
import com.artinus.subscription.domain.subscription.SubscriptionStatus;
import com.artinus.subscription.infrastructure.persistence.adapter.JpaChannelAdapter;
import com.artinus.subscription.infrastructure.persistence.adapter.JpaIdempotencyAdapter;
import com.artinus.subscription.infrastructure.persistence.adapter.JpaMemberAdapter;
import com.artinus.subscription.infrastructure.persistence.adapter.JpaSubscriptionHistoryAdapter;
import com.artinus.subscription.infrastructure.persistence.entity.JpaChannelEntity;
import com.artinus.subscription.infrastructure.persistence.entity.JpaIdempotencyKeyEntity;
import com.artinus.subscription.infrastructure.persistence.entity.JpaMemberEntity;
import com.artinus.subscription.infrastructure.persistence.entity.JpaSubscriptionHistoryEntity;
import com.artinus.subscription.infrastructure.persistence.repository.ChannelRepository;
import com.artinus.subscription.infrastructure.persistence.repository.IdempotencyKeyRepository;
import com.artinus.subscription.infrastructure.persistence.repository.MemberRepository;
import com.artinus.subscription.infrastructure.persistence.repository.SubscriptionHistoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

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

	@Test
	void idempotencyAdapterSavesAndFindsCompletedResult() {
		JpaIdempotencyAdapter adapter = new JpaIdempotencyAdapter(idempotencyKeyRepository, new ObjectMapper());
		SubscriptionResult response = new SubscriptionResult("01012345678", SubscriptionStatus.BASIC);

		adapter.saveCompleted(new CompletedIdempotency("01012345678", "adapter-key", "request-hash", response));

		assertThat(adapter.findCompleted("01012345678", "adapter-key"))
			.hasValueSatisfying(found -> {
				assertThat(found.requestHash()).isEqualTo("request-hash");
				assertThat(found.response()).isEqualTo(response);
			});
	}

	@Test
	void memberAdapterSavesAndFindsMemberByPhoneNumber() {
		JpaMemberAdapter adapter = new JpaMemberAdapter(memberRepository);
		Member member = Member.create("010-1234-5678", SubscriptionStatus.BASIC);

		adapter.save(member);

		assertThat(adapter.findByPhoneNumber("01012345678"))
			.hasValueSatisfying(found -> assertThat(found.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.BASIC));
	}

	@Test
	void channelAdapterFindsChannelById() {
		JpaChannelAdapter adapter = new JpaChannelAdapter(channelRepository);

		Channel channel = adapter.getById(1L);

		assertThat(channel.name()).isEqualTo("홈페이지");
		assertThat(channel.supportsSubscribe()).isTrue();
	}

	@Test
	void historyAdapterSavesSubscriptionHistory() {
		JpaMemberAdapter memberAdapter = new JpaMemberAdapter(memberRepository);
		JpaChannelAdapter channelAdapter = new JpaChannelAdapter(channelRepository);
		JpaSubscriptionHistoryAdapter historyAdapter = new JpaSubscriptionHistoryAdapter(
			memberRepository,
			channelRepository,
			historyRepository
		);
		Member member = memberAdapter.save(Member.create("01012345678", SubscriptionStatus.BASIC));
		Channel channel = channelAdapter.getById(5L);

		historyAdapter.save(SubscriptionHistory.record(member, channel, SubscriptionActionType.CANCEL,
			SubscriptionStatus.BASIC, SubscriptionStatus.NONE, LocalDateTime.now()));

		assertThat(historyRepository.findAll())
			.extracting(JpaSubscriptionHistoryEntity::getActionType)
			.containsExactly(SubscriptionActionType.CANCEL);
	}

	@Test
	void historyAdapterFindsHistoriesByPhoneNumber() {
		JpaMemberEntity member = memberRepository.save(JpaMemberEntity.create("01012345678", SubscriptionStatus.NONE));
		JpaChannelEntity homepage = channelRepository.findByName("홈페이지").orElseThrow();
		JpaChannelEntity mobileApp = channelRepository.findByName("모바일앱").orElseThrow();
		historyRepository.save(JpaSubscriptionHistoryEntity.record(member, homepage, SubscriptionActionType.SUBSCRIBE,
			SubscriptionStatus.NONE, SubscriptionStatus.BASIC, LocalDateTime.of(2026, 1, 1, 10, 0)));
		historyRepository.save(JpaSubscriptionHistoryEntity.record(member, mobileApp, SubscriptionActionType.SUBSCRIBE,
			SubscriptionStatus.BASIC, SubscriptionStatus.PREMIUM, LocalDateTime.of(2026, 1, 2, 10, 0)));
		JpaSubscriptionHistoryAdapter historyAdapter = new JpaSubscriptionHistoryAdapter(
			memberRepository,
			channelRepository,
			historyRepository
		);

		List<SubscriptionHistory> histories = historyAdapter.findByPhoneNumber("01012345678");

		assertThat(histories)
			.extracting(history -> history.channel().name())
			.containsExactly("홈페이지", "모바일앱");
	}
}
