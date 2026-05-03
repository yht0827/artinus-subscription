package com.artinus.subscription.api.response;

import com.artinus.subscription.application.result.SubscriptionResult;
import com.artinus.subscription.domain.subscription.SubscriptionStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "구독 상태 변경 응답")
public record SubscriptionCommandResponse(
	@Schema(description = "정규화된 회원 휴대폰번호", example = "01012345678")
	String phoneNumber,

	@Schema(description = "현재 구독 상태", example = "BASIC")
	SubscriptionStatus subscriptionStatus
) {

	public static SubscriptionCommandResponse from(SubscriptionResult result) {
		return new SubscriptionCommandResponse(result.phoneNumber(), result.subscriptionStatus());
	}
}
