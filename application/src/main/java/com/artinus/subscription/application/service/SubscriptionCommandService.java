package com.artinus.subscription.application.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.transaction.annotation.Transactional;

import com.artinus.subscription.application.command.CancelCommand;
import com.artinus.subscription.application.command.SubscribeCommand;
import com.artinus.subscription.application.exception.ExternalApprovalRejectedException;
import com.artinus.subscription.application.exception.MemberNotFoundException;
import com.artinus.subscription.application.idempotency.IdempotencyContext;
import com.artinus.subscription.application.idempotency.IdempotencyProcessor;
import com.artinus.subscription.application.port.in.SubscriptionCommandUseCase;
import com.artinus.subscription.application.port.out.ChannelPort;
import com.artinus.subscription.application.port.out.ExternalApprovalPort;
import com.artinus.subscription.application.port.out.MemberPort;
import com.artinus.subscription.application.port.out.SubscriptionHistoryPort;
import com.artinus.subscription.application.result.SubscriptionResult;
import com.artinus.subscription.domain.channel.Channel;
import com.artinus.subscription.domain.history.SubscriptionHistory;
import com.artinus.subscription.domain.member.Member;
import com.artinus.subscription.domain.member.PhoneNumber;
import com.artinus.subscription.domain.subscription.SubscriptionActionType;
import com.artinus.subscription.domain.subscription.SubscriptionStatus;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SubscriptionCommandService implements SubscriptionCommandUseCase {

	private final MemberPort memberPort;
	private final ChannelPort channelPort;
	private final SubscriptionHistoryPort historyPort;
	private final ExternalApprovalPort externalApprovalPort;
	private final IdempotencyProcessor idempotencyProcessor;

	@Override
	@Transactional
	public SubscriptionResult subscribe(SubscribeCommand command) {
		String phoneNumber = PhoneNumber.from(command.phoneNumber()).value();

		// 멱등성 키로 이미 완료된 요청인지 확인
		IdempotencyContext idempotency = idempotencyProcessor.createContext(SubscriptionActionType.SUBSCRIBE,
			phoneNumber,
			command.channelId(), command.targetStatus(), command.idempotencyKey());
		Optional<SubscriptionResult> completedResult = idempotencyProcessor.findCompletedResult(idempotency);
		if (completedResult.isPresent()) {
			return completedResult.get();
		}

		Member member = memberPort.findByPhoneNumber(phoneNumber)
			.orElseGet(() -> memberPort.save(Member.create(phoneNumber, SubscriptionStatus.NONE)));
		Channel channel = channelPort.getById(command.channelId());

		// 도메인 규칙 검증 후 외부 승인 요청
		member.validateSubscribe(channel, command.targetStatus());
		approveExternally();

		// 상태 변경, 이력 저장, 멱등성 결과 저장
		SubscriptionResult result = changeStatusAndRecordHistory(
			member.subscribe(channel, command.targetStatus(), LocalDateTime.now())
		);
		idempotencyProcessor.saveCompletedResult(idempotency, result);
		return result;
	}

	@Override
	@Transactional
	public SubscriptionResult cancel(CancelCommand command) {
		String phoneNumber = PhoneNumber.from(command.phoneNumber()).value();

		// 멱등성 키로 이미 완료된 요청인지 확인
		IdempotencyContext idempotency = idempotencyProcessor.createContext(SubscriptionActionType.CANCEL, phoneNumber,
			command.channelId(), command.targetStatus(), command.idempotencyKey());
		Optional<SubscriptionResult> completedResult = idempotencyProcessor.findCompletedResult(idempotency);
		if (completedResult.isPresent()) {
			return completedResult.get();
		}

		Member member = memberPort.findByPhoneNumber(phoneNumber)
			.orElseThrow(MemberNotFoundException::new);
		Channel channel = channelPort.getById(command.channelId());

		// 도메인 규칙 검증 후 외부 승인 요청
		member.validateCancel(channel, command.targetStatus());
		approveExternally();

		// 상태 변경, 이력 저장, 멱등성 결과 저장
		SubscriptionResult result = changeStatusAndRecordHistory(
			member.cancel(channel, command.targetStatus(), LocalDateTime.now())
		);
		idempotencyProcessor.saveCompletedResult(idempotency, result);
		return result;
	}

	private SubscriptionResult changeStatusAndRecordHistory(SubscriptionHistory history) {
		Member member = history.member();
		memberPort.save(member);
		historyPort.save(history);
		return new SubscriptionResult(member.getPhoneNumber(), member.getSubscriptionStatus());
	}

	private void approveExternally() {
		if (!externalApprovalPort.approve()) {
			throw new ExternalApprovalRejectedException();
		}
	}
}
