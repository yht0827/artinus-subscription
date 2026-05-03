package com.artinus.subscription.infrastructure.llm;

import java.util.List;

import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.artinus.subscription.application.port.out.HistorySummaryPort;
import com.artinus.subscription.domain.history.SubscriptionHistory;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OpenAiHistorySummaryClient implements HistorySummaryPort {

	private static final String RESPONSES_PATH = "/v1/responses";

	private final RestClient restClient;
	private final HistorySummaryPort fallbackSummaryPort;
	private final OpenAiHistorySummaryRequestFactory requestFactory;
	private final OpenAiResponseTextExtractor responseTextExtractor;

	public OpenAiHistorySummaryClient(RestClient.Builder restClientBuilder, HistorySummaryPort fallbackSummaryPort,
		String apiKey, String model, int maxOutputTokens) {
		this.restClient = restClientBuilder
			.baseUrl("https://api.openai.com")
			.defaultHeader("Authorization", "Bearer " + apiKey)
			.build();
		this.fallbackSummaryPort = fallbackSummaryPort;
		this.requestFactory = new OpenAiHistorySummaryRequestFactory(model, maxOutputTokens);
		this.responseTextExtractor = new OpenAiResponseTextExtractor();
	}

	@Override
	public String summarize(List<SubscriptionHistory> histories) {
		if (histories.isEmpty()) {
			return fallbackSummaryPort.summarize(histories);
		}
		try {
			JsonNode response = restClient.post()
				.uri(RESPONSES_PATH)
				.body(requestFactory.create(histories))
				.retrieve()
				.body(JsonNode.class);
			String summary = responseTextExtractor.extract(response);
			if (summary.isBlank()) {
				return fallback(histories);
			}
			return summary;
		} catch (RestClientException | IllegalArgumentException exception) {
			log.warn("OpenAI 구독 이력 요약에 실패해 fallback 요약을 반환합니다.", exception);
			return fallback(histories);
		}
	}

	private String fallback(List<SubscriptionHistory> histories) {
		return fallbackSummaryPort.summarize(histories);
	}
}
