package com.artinus.subscription.infrastructure.persistence;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.artinus.subscription.application.result.CompletedIdempotency;
import com.artinus.subscription.application.result.SubscriptionResult;
import com.artinus.subscription.domain.channel.Channel;
import com.artinus.subscription.domain.history.SubscriptionHistory;
import com.artinus.subscription.domain.member.Member;
import com.artinus.subscription.domain.subscription.SubscriptionActionType;
import com.artinus.subscription.domain.subscription.SubscriptionStatus;
import com.artinus.subscription.infrastructure.TestcontainersConfiguration;
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
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SubscriptionPersistenceTest {

	@DynamicPropertySource
	static void configureDatasource(DynamicPropertyRegistry registry) {
		TestcontainersConfiguration.startMysql(registry);
	}

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
		// when
		List<JpaChannelEntity> channels = channelRepository.findAll();

		// then
		assertThat(channels).hasSize(6);
		assertThat(channels)
			.extracting(JpaChannelEntity::getName)
			.containsExactlyInAnyOrder("홈페이지", "모바일앱", "네이버", "SKT", "콜센터", "이메일");
	}

	@Test
	void savesAndFindsMemberByPhoneNumber() {
		// given
		JpaMemberEntity member = JpaMemberEntity.create("01012345678", SubscriptionStatus.NONE);

		// when
		memberRepository.save(member);

		// then
		assertThat(memberRepository.findByPhoneNumber("01012345678"))
			.hasValueSatisfying(found -> assertThat(found.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.NONE));
	}

	@Test
	void savesSubscriptionHistoryAndFindsByMemberIdOrderedByChangedAt() {
		// given
		JpaMemberEntity member = memberRepository.save(JpaMemberEntity.create("01012345678", SubscriptionStatus.NONE));
		JpaChannelEntity homepage = channelRepository.findByName("홈페이지").orElseThrow();
		JpaChannelEntity callCenter = channelRepository.findByName("콜센터").orElseThrow();

		// when
		historyRepository.save(JpaSubscriptionHistoryEntity.record(member, homepage, SubscriptionActionType.SUBSCRIBE,
			SubscriptionStatus.NONE, SubscriptionStatus.BASIC, LocalDateTime.of(2026, 1, 1, 10, 0)));
		historyRepository.save(JpaSubscriptionHistoryEntity.record(member, callCenter, SubscriptionActionType.CANCEL,
			SubscriptionStatus.BASIC, SubscriptionStatus.NONE, LocalDateTime.of(2026, 1, 2, 10, 0)));

		List<JpaSubscriptionHistoryEntity> histories = historyRepository.findByMemberIdOrderByChangedAtAsc(
			member.getId());

		// then
		assertThat(histories)
			.extracting(JpaSubscriptionHistoryEntity::getActionType)
			.containsExactly(SubscriptionActionType.SUBSCRIBE, SubscriptionActionType.CANCEL);
	}

	@Test
	void savesAndFindsIdempotencyKeyByPhoneNumberAndKey() {
		// given
		JpaIdempotencyKeyEntity idempotencyKey = JpaIdempotencyKeyEntity.completed(
			"01012345678",
			"request-key",
			"request-hash",
			"{\"subscriptionStatus\":\"BASIC\"}",
			200
		);

		// when
		idempotencyKeyRepository.save(idempotencyKey);

		// then
		assertThat(idempotencyKeyRepository.findByPhoneNumberAndIdempotencyKey("01012345678", "request-key"))
			.hasValueSatisfying(found -> assertThat(found.getRequestHash()).isEqualTo("request-hash"));
	}

	@Test
	void idempotencyAdapterSavesAndFindsCompletedResult() {
		// given
		JpaIdempotencyAdapter adapter = new JpaIdempotencyAdapter(idempotencyKeyRepository, new ObjectMapper());
		SubscriptionResult response = new SubscriptionResult("01012345678", SubscriptionStatus.BASIC);

		// when
		adapter.saveCompleted(new CompletedIdempotency("01012345678", "adapter-key", "request-hash", response));

		// then
		assertThat(adapter.findCompleted("01012345678", "adapter-key"))
			.hasValueSatisfying(found -> {
				assertThat(found.requestHash()).isEqualTo("request-hash");
				assertThat(found.response()).isEqualTo(response);
			});
	}

	@Test
	void memberAdapterSavesAndFindsMemberByPhoneNumber() {
		// given
		JpaMemberAdapter adapter = new JpaMemberAdapter(memberRepository);
		Member member = Member.create("010-1234-5678", SubscriptionStatus.BASIC);

		// when
		adapter.save(member);

		// then
		assertThat(adapter.findByPhoneNumber("01012345678"))
			.hasValueSatisfying(found -> assertThat(found.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.BASIC));
	}

	@Test
	void channelAdapterFindsChannelById() {
		// given
		JpaChannelAdapter adapter = new JpaChannelAdapter(channelRepository);

		// when
		Channel channel = adapter.getById(1L);

		// then
		assertThat(channel.name()).isEqualTo("홈페이지");
		assertThat(channel.supportsSubscribe()).isTrue();
	}

	@Test
	void historyAdapterSavesSubscriptionHistory() {
		// given
		JpaMemberAdapter memberAdapter = new JpaMemberAdapter(memberRepository);
		JpaChannelAdapter channelAdapter = new JpaChannelAdapter(channelRepository);
		JpaSubscriptionHistoryAdapter historyAdapter = new JpaSubscriptionHistoryAdapter(
			memberRepository,
			channelRepository,
			historyRepository
		);
		Member member = memberAdapter.save(Member.create("01012345678", SubscriptionStatus.BASIC));
		Channel channel = channelAdapter.getById(5L);

		// when
		historyAdapter.save(SubscriptionHistory.record(member, channel, SubscriptionActionType.CANCEL,
			SubscriptionStatus.BASIC, SubscriptionStatus.NONE, LocalDateTime.now()));

		// then
		assertThat(historyRepository.findAll())
			.extracting(JpaSubscriptionHistoryEntity::getActionType)
			.containsExactly(SubscriptionActionType.CANCEL);
	}

	@Test
	void historyAdapterFindsHistoriesByPhoneNumber() {
		// given
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

		// when
		List<SubscriptionHistory> histories = historyAdapter.findByPhoneNumber("01012345678");

		// then
		assertThat(histories)
			.extracting(history -> history.channel().name())
			.containsExactly("홈페이지", "모바일앱");
	}
}
