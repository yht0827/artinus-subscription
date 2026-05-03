package com.artinus.subscription.api.request;

import com.artinus.subscription.application.command.CancelCommand;
import com.artinus.subscription.application.command.SubscribeCommand;
import com.artinus.subscription.domain.subscription.SubscriptionStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "구독 상태 변경 요청")
public record SubscriptionCommandRequest(
	@Schema(description = "회원 휴대폰번호", example = "010-1234-5678")
	@NotBlank(message = "휴대폰번호는 필수입니다.")
	String phoneNumber,

	@Schema(description = "요청 채널 ID", example = "1")
	@NotNull(message = "채널 ID는 필수입니다.")
	Long channelId,

	@Schema(description = "변경할 구독 상태", example = "BASIC")
	@NotNull(message = "변경할 구독 상태는 필수입니다.")
	SubscriptionStatus targetStatus
) {

	public SubscribeCommand toSubscribeCommand(String idempotencyKey) {
		return new SubscribeCommand(phoneNumber, channelId, targetStatus, idempotencyKey);
	}

	public CancelCommand toCancelCommand(String idempotencyKey) {
		return new CancelCommand(phoneNumber, channelId, targetStatus, idempotencyKey);
	}
}
