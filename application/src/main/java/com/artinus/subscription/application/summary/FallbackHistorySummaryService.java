package com.artinus.subscription.application.summary;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import com.artinus.subscription.application.port.out.HistorySummaryPort;
import com.artinus.subscription.domain.history.SubscriptionHistory;
import com.artinus.subscription.domain.subscription.SubscriptionActionType;
import com.artinus.subscription.domain.subscription.SubscriptionStatus;

public class FallbackHistorySummaryService implements HistorySummaryPort {

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy년 M월 d일");
	private static final String EMPTY_HISTORY_MESSAGE = "구독 이력이 없습니다.";
	private static final String HISTORY_SENTENCE_FORMAT = "%s %s를 통해 %s으로 %s하였습니다.";

	@Override
	public String summarize(List<SubscriptionHistory> histories) {
		if (histories.isEmpty()) {
			return EMPTY_HISTORY_MESSAGE;
		}
		return histories.stream()
			.map(this::toSentence)
			.collect(Collectors.joining(" "));
	}

	private String toSentence(SubscriptionHistory history) {
		return HISTORY_SENTENCE_FORMAT.formatted(
			history.changedAt().format(DATE_FORMATTER),
			history.channel().name(),
			statusLabel(history.afterStatus()),
			actionLabel(history.actionType())
		);
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
