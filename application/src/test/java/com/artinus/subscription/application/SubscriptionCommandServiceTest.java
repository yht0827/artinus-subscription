package com.artinus.subscription.application;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.artinus.subscription.application.command.CancelCommand;
import com.artinus.subscription.application.command.SubscribeCommand;
import com.artinus.subscription.application.exception.ExternalApprovalRejectedException;
import com.artinus.subscription.application.exception.IdempotencyConflictException;
import com.artinus.subscription.application.idempotency.IdempotencyProcessor;
import com.artinus.subscription.application.port.out.ChannelPort;
import com.artinus.subscription.application.port.out.ExternalApprovalPort;
import com.artinus.subscription.application.port.out.IdempotencyPort;
import com.artinus.subscription.application.port.out.MemberPort;
import com.artinus.subscription.application.port.out.SubscriptionHistoryPort;
import com.artinus.subscription.application.result.CompletedIdempotency;
import com.artinus.subscription.application.result.SubscriptionResult;
import com.artinus.subscription.application.service.SubscriptionCommandService;
import com.artinus.subscription.domain.channel.Channel;
import com.artinus.subscription.domain.exception.InvalidSubscriptionTransitionException;
import com.artinus.subscription.domain.history.SubscriptionHistory;
import com.artinus.subscription.domain.member.Member;
import com.artinus.subscription.domain.subscription.SubscriptionActionType;
import com.artinus.subscription.domain.subscription.SubscriptionStatus;

class SubscriptionCommandServiceTest {

	private final FakeMemberPort memberPort = new FakeMemberPort();
	private final FakeChannelPort channelPort = new FakeChannelPort();
	private final FakeSubscriptionHistoryPort historyPort = new FakeSubscriptionHistoryPort();
	private final FakeExternalApprovalPort externalApprovalPort = new FakeExternalApprovalPort();
	private final FakeIdempotencyPort idempotencyPort = new FakeIdempotencyPort();
	private final IdempotencyProcessor idempotencyProcessor = new IdempotencyProcessor(idempotencyPort);
	private final SubscriptionCommandService service = new SubscriptionCommandService(
		memberPort,
		channelPort,
		historyPort,
		externalApprovalPort,
		idempotencyProcessor
	);

	@Test
	void subscribesNewMemberToBasicAndStoresHistory() {
		// given
		SubscribeCommand command = new SubscribeCommand("010-1234-5678", 1L, SubscriptionStatus.BASIC, "key-1");

		// when
		SubscriptionResult result = service.subscribe(command);

		// then
		assertThat(result.phoneNumber()).isEqualTo("01012345678");
		assertThat(result.subscriptionStatus()).isEqualTo(SubscriptionStatus.BASIC);
		assertThat(memberPort.findByPhoneNumber("01012345678"))
			.hasValueSatisfying(
				member -> assertThat(member.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.BASIC));
		assertThat(historyPort.histories)
			.extracting(SubscriptionHistory::actionType)
			.containsExactly(SubscriptionActionType.SUBSCRIBE);
	}

	@Test
	void cancelsExistingBasicMemberToNoneAndStoresHistory() {
		// given
		memberPort.save(Member.create("01012345678", SubscriptionStatus.BASIC));
		CancelCommand command = new CancelCommand("01012345678", 5L, SubscriptionStatus.NONE, "key-2");

		// when
		SubscriptionResult result = service.cancel(command);

		// then
		assertThat(result.subscriptionStatus()).isEqualTo(SubscriptionStatus.NONE);
		assertThat(historyPort.histories)
			.extracting(SubscriptionHistory::actionType)
			.containsExactly(SubscriptionActionType.CANCEL);
	}

	@Test
	void doesNotCallExternalApprovalWhenTransitionIsInvalid() {
		// given
		memberPort.save(Member.create("01012345678", SubscriptionStatus.BASIC));
		SubscribeCommand command = new SubscribeCommand("01012345678", 1L, SubscriptionStatus.NONE, "key-3");

		// when & then
		assertThatThrownBy(() -> service.subscribe(command))
			.isInstanceOf(InvalidSubscriptionTransitionException.class);

		assertThat(externalApprovalPort.callCount).isZero();
		assertThat(historyPort.histories).isEmpty();
	}

	@Test
	void rejectsSubscribeWhenExternalApprovalFails() {
		// given
		externalApprovalPort.approved = false;
		SubscribeCommand command = new SubscribeCommand("01012345678", 1L, SubscriptionStatus.BASIC, "key-4");

		// when & then
		assertThatThrownBy(() -> service.subscribe(command))
			.isInstanceOf(ExternalApprovalRejectedException.class);

		assertThat(memberPort.findByPhoneNumber("01012345678"))
			.hasValueSatisfying(
				member -> assertThat(member.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.NONE));
		assertThat(historyPort.histories).isEmpty();
	}

	@Test
	void returnsStoredResultWhenSubscribeRequestIsRepeatedWithSameIdempotencyKey() {
		// given
		SubscribeCommand command = new SubscribeCommand("01012345678", 1L, SubscriptionStatus.BASIC, "key-repeat");

		// when
		SubscriptionResult first = service.subscribe(command);
		SubscriptionResult second = service.subscribe(command);

		// then
		assertThat(second).isEqualTo(first);
		assertThat(externalApprovalPort.callCount).isEqualTo(1);
		assertThat(historyPort.histories).hasSize(1);
	}

	@Test
	void rejectsRepeatedSubscribeRequestWithSameIdempotencyKeyButDifferentBody() {
		// given
		service.subscribe(new SubscribeCommand("01012345678", 1L, SubscriptionStatus.BASIC, "key-conflict"));

		// when & then
		assertThatThrownBy(() -> service.subscribe(
			new SubscribeCommand("01012345678", 1L, SubscriptionStatus.PREMIUM, "key-conflict")
		)).isInstanceOf(IdempotencyConflictException.class);

		assertThat(externalApprovalPort.callCount).isEqualTo(1);
		assertThat(historyPort.histories).hasSize(1);
	}

	@Test
	void doesNotStoreIdempotencyResultWhenExternalApprovalFails() {
		// given
		SubscribeCommand command = new SubscribeCommand("01012345678", 1L, SubscriptionStatus.BASIC, "key-retry");
		externalApprovalPort.approved = false;

		// when & then
		assertThatThrownBy(() -> service.subscribe(command))
			.isInstanceOf(ExternalApprovalRejectedException.class);

		// when
		externalApprovalPort.approved = true;
		SubscriptionResult result = service.subscribe(command);

		// then
		assertThat(result.subscriptionStatus()).isEqualTo(SubscriptionStatus.BASIC);
		assertThat(externalApprovalPort.callCount).isEqualTo(2);
		assertThat(historyPort.histories).hasSize(1);
	}

	private static class FakeMemberPort implements MemberPort {

		private final Map<String, Member> members = new HashMap<>();

		@Override
		public Optional<Member> findByPhoneNumber(String phoneNumber) {
			return Optional.ofNullable(members.get(phoneNumber));
		}

		@Override
		public Member save(Member member) {
			members.put(member.getPhoneNumber(), member);
			return member;
		}
	}

	private static class FakeChannelPort implements ChannelPort {

		private final Map<Long, Channel> channels = Map.of(
			1L, new Channel(1L, "홈페이지", true, true),
			5L, new Channel(5L, "콜센터", false, true)
		);

		@Override
		public Channel getById(Long channelId) {
			return channels.get(channelId);
		}
	}

	private static class FakeSubscriptionHistoryPort implements SubscriptionHistoryPort {

		private final List<SubscriptionHistory> histories = new ArrayList<>();

		@Override
		public void save(SubscriptionHistory history) {
			histories.add(history);
		}

		@Override
		public List<SubscriptionHistory> findByPhoneNumber(String phoneNumber) {
			return histories.stream()
				.filter(history -> history.member().getPhoneNumber().equals(phoneNumber))
				.toList();
		}
	}

	private static class FakeExternalApprovalPort implements ExternalApprovalPort {

		private boolean approved = true;
		private int callCount;

		@Override
		public boolean approve() {
			callCount++;
			return approved;
		}
	}

	private static class FakeIdempotencyPort implements IdempotencyPort {

		private final Map<String, CompletedIdempotency> results = new HashMap<>();

		@Override
		public Optional<CompletedIdempotency> findCompleted(String phoneNumber, String idempotencyKey) {
			return Optional.ofNullable(results.get(key(phoneNumber, idempotencyKey)));
		}

		@Override
		public void saveCompleted(CompletedIdempotency completed) {
			results.put(key(completed.phoneNumber(), completed.idempotencyKey()), completed);
		}

		private String key(String phoneNumber, String idempotencyKey) {
			return phoneNumber + ":" + idempotencyKey;
		}
	}
}
