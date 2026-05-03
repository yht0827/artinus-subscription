package com.artinus.subscription.api.response;

import java.time.LocalDateTime;
import java.util.List;

import com.artinus.subscription.application.result.SubscriptionHistoryResult;
import com.artinus.subscription.domain.subscription.SubscriptionActionType;
import com.artinus.subscription.domain.subscription.SubscriptionStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "구독 이력 조회 응답")
public record SubscriptionHistoryResponse(
	@Schema(description = "구독 이력 목록")
	List<HistoryItemResponse> history,

	@Schema(description = "구독 이력을 기반으로 생성한 자연어 요약", example = "2026년 1월 1일 홈페이지를 통해 일반 구독으로 구독하였습니다.")
	String summary
) {

	public static SubscriptionHistoryResponse from(SubscriptionHistoryResult result) {
		return new SubscriptionHistoryResponse(
			result.history().stream()
				.map(HistoryItemResponse::from)
				.toList(),
			result.summary()
		);
	}

	@Schema(description = "구독 이력 항목")
	public record HistoryItemResponse(
		@Schema(description = "채널명", example = "홈페이지")
		String channelName,

		@Schema(description = "구독 작업 유형", example = "SUBSCRIBE")
		SubscriptionActionType actionType,

		@Schema(description = "변경 전 구독 상태", example = "NONE")
		SubscriptionStatus beforeStatus,

		@Schema(description = "변경 후 구독 상태", example = "BASIC")
		SubscriptionStatus afterStatus,

		@Schema(description = "변경 일시", example = "2026-01-01T10:00:00")
		LocalDateTime changedAt
	) {

		private static HistoryItemResponse from(SubscriptionHistoryResult.HistoryItem item) {
			return new HistoryItemResponse(item.channelName(), item.actionType(), item.beforeStatus(),
				item.afterStatus(), item.changedAt());
		}
	}
}
