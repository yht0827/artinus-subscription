package com.artinus.subscription.application;

import java.time.LocalDateTime;

import com.artinus.subscription.application.command.CancelCommand;
import com.artinus.subscription.application.command.SubscribeCommand;
import com.artinus.subscription.application.port.ChannelPort;
import com.artinus.subscription.application.port.ExternalApprovalPort;
import com.artinus.subscription.application.port.MemberPort;
import com.artinus.subscription.application.port.SubscriptionHistoryPort;
import com.artinus.subscription.application.result.SubscriptionResult;
import com.artinus.subscription.domain.channel.Channel;
import com.artinus.subscription.domain.history.SubscriptionHistory;
import com.artinus.subscription.domain.member.Member;
import com.artinus.subscription.domain.member.PhoneNumber;
import com.artinus.subscription.domain.subscription.SubscriptionActionType;
import com.artinus.subscription.domain.subscription.SubscriptionStatus;
import com.artinus.subscription.domain.subscription.SubscriptionTransitionPolicy;

public class SubscriptionCommandService {

	private final MemberPort memberPort;
	private final ChannelPort channelPort;
	private final SubscriptionHistoryPort historyPort;
	private final ExternalApprovalPort externalApprovalPort;
	private final SubscriptionTransitionPolicy transitionPolicy = new SubscriptionTransitionPolicy();

	public SubscriptionCommandService(MemberPort memberPort, ChannelPort channelPort,
		SubscriptionHistoryPort historyPort, ExternalApprovalPort externalApprovalPort) {
		this.memberPort = memberPort;
		this.channelPort = channelPort;
		this.historyPort = historyPort;
		this.externalApprovalPort = externalApprovalPort;
	}

	public SubscriptionResult subscribe(SubscribeCommand command) {
		String phoneNumber = PhoneNumber.from(command.phoneNumber()).value();
		Member member = memberPort.findByPhoneNumber(phoneNumber)
			.orElseGet(() -> memberPort.save(Member.create(phoneNumber, SubscriptionStatus.NONE)));
		Channel channel = channelPort.getById(command.channelId());

		validateSubscribeChannel(channel);
		SubscriptionStatus beforeStatus = member.getSubscriptionStatus();
		transitionPolicy.validateSubscribe(beforeStatus, command.targetStatus());
		approveExternally();

		member.changeStatus(command.targetStatus());
		memberPort.save(member);
		historyPort.save(SubscriptionHistory.record(member, channel, SubscriptionActionType.SUBSCRIBE,
			beforeStatus, command.targetStatus(), LocalDateTime.now()));
		return new SubscriptionResult(member.getPhoneNumber(), member.getSubscriptionStatus());
	}

	public SubscriptionResult cancel(CancelCommand command) {
		String phoneNumber = PhoneNumber.from(command.phoneNumber()).value();
		Member member = memberPort.findByPhoneNumber(phoneNumber)
			.orElseThrow(() -> new IllegalArgumentException("Member not found."));
		Channel channel = channelPort.getById(command.channelId());

		validateCancelChannel(channel);
		SubscriptionStatus beforeStatus = member.getSubscriptionStatus();
		transitionPolicy.validateCancel(beforeStatus, command.targetStatus());
		approveExternally();

		member.changeStatus(command.targetStatus());
		memberPort.save(member);
		historyPort.save(SubscriptionHistory.record(member, channel, SubscriptionActionType.CANCEL,
			beforeStatus, command.targetStatus(), LocalDateTime.now()));
		return new SubscriptionResult(member.getPhoneNumber(), member.getSubscriptionStatus());
	}

	private void validateSubscribeChannel(Channel channel) {
		if (channel == null || !channel.supportsSubscribe()) {
			throw new ChannelPermissionException("Channel does not support subscribe.");
		}
	}

	private void validateCancelChannel(Channel channel) {
		if (channel == null || !channel.supportsCancel()) {
			throw new ChannelPermissionException("Channel does not support cancel.");
		}
	}

	private void approveExternally() {
		if (!externalApprovalPort.approve()) {
			throw new ExternalApprovalRejectedException();
		}
	}
}
