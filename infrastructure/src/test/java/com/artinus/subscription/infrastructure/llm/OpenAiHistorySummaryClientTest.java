package com.artinus.subscription.infrastructure.llm;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.artinus.subscription.application.port.out.HistorySummaryPort;
import com.artinus.subscription.domain.channel.Channel;
import com.artinus.subscription.domain.history.SubscriptionHistory;
import com.artinus.subscription.domain.member.Member;
import com.artinus.subscription.domain.subscription.SubscriptionActionType;
import com.artinus.subscription.domain.subscription.SubscriptionStatus;

class OpenAiHistorySummaryClientTest {

	private static final String OPENAI_RESPONSES_URL = "https://api.openai.com/v1/responses";

	private MockRestServiceServer server;
	private OpenAiHistorySummaryClient client;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		client = new OpenAiHistorySummaryClient(builder.baseUrl("https://api.openai.com")
			.defaultHeader("Authorization", "Bearer test-api-key")
			.build(),
			new FakeFallbackSummaryPort(), "gpt-4.1-mini", 180);
	}

	@Test
	void summarizesHistoriesWithOpenAiResponse() {
		// given
		server.expect(requestTo(OPENAI_RESPONSES_URL))
			.andExpect(header("Authorization", "Bearer test-api-key"))
			.andRespond(withSuccess("""
				{
				  "output_text": "2026년 1월 1일 홈페이지에서 일반 구독으로 변경되었습니다."
				}
				""", MediaType.APPLICATION_JSON));

		// when
		String summary = client.summarize(histories());

		// then
		assertThat(summary).isEqualTo("2026년 1월 1일 홈페이지에서 일반 구독으로 변경되었습니다.");
		server.verify();
	}

	@Test
	void returnsFallbackSummaryWhenOpenAiRequestFails() {
		// given
		server.expect(requestTo(OPENAI_RESPONSES_URL))
			.andRespond(withServerError());

		// when
		String summary = client.summarize(histories());

		// then
		assertThat(summary).isEqualTo("fallback summary");
		server.verify();
	}

	@Test
	void returnsFallbackSummaryWithoutCallingOpenAiWhenHistoryIsEmpty() {
		// when
		String summary = client.summarize(List.of());

		// then
		assertThat(summary).isEqualTo("fallback summary");
		server.verify();
	}

	private List<SubscriptionHistory> histories() {
		Member member = Member.create("01012345678", SubscriptionStatus.BASIC);
		Channel homepage = new Channel(1L, "홈페이지", true, true);
		return List.of(SubscriptionHistory.record(member, homepage, SubscriptionActionType.SUBSCRIBE,
			SubscriptionStatus.NONE, SubscriptionStatus.BASIC, LocalDateTime.of(2026, 1, 1, 10, 0)));
	}

	private static class FakeFallbackSummaryPort implements HistorySummaryPort {

		@Override
		public String summarize(List<SubscriptionHistory> histories) {
			return "fallback summary";
		}
	}
}
