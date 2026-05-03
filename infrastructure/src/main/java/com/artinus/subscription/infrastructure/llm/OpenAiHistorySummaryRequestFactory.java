package com.artinus.subscription.infrastructure.llm;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.artinus.subscription.domain.history.SubscriptionHistory;
import com.artinus.subscription.domain.subscription.SubscriptionActionType;
import com.artinus.subscription.domain.subscription.SubscriptionStatus;

public class OpenAiHistorySummaryRequestFactory {

	private static final String SUMMARY_INSTRUCTIONS = """
		구독 이력을 한국어 한두 문장으로 자연스럽게 요약하세요.
		날짜, 채널, 변경된 구독 상태를 포함하고 과장하지 마세요.
		""";

	private final String model;
	private final int maxOutputTokens;

	public OpenAiHistorySummaryRequestFactory(String model, int maxOutputTokens) {
		this.model = model;
		this.maxOutputTokens = maxOutputTokens;
	}

	public Map<String, Object> create(List<SubscriptionHistory> histories) {
		return Map.of(
			"model", model,
			"instructions", SUMMARY_INSTRUCTIONS,
			"input", historyInput(histories),
			"max_output_tokens", maxOutputTokens
		);
	}

	private String historyInput(List<SubscriptionHistory> histories) {
		return histories.stream()
			.map(history -> "변경일시=%s, 채널=%s, 작업=%s, 변경전=%s, 변경후=%s".formatted(
				history.changedAt(),
				history.channel().name(),
				actionLabel(history.actionType()),
				statusLabel(history.beforeStatus()),
				statusLabel(history.afterStatus())
			))
			.collect(Collectors.joining("\n"));
	}

	private String actionLabel(SubscriptionActionType actionType) {
		return switch (actionType) {
			case SUBSCRIBE -> "구독";
			case CANCEL -> "구독 해지";
		};
	}

	private String statusLabel(SubscriptionStatus status) {
		return switch (status) {
			case NONE -> "구독 안함";
			case BASIC -> "일반 구독";
			case PREMIUM -> "프리미엄 구독";
		};
	}
}
