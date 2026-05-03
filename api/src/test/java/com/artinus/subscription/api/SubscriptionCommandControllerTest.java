package com.artinus.subscription.api;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.artinus.subscription.application.port.in.SubscriptionCommandUseCase;
import com.artinus.subscription.application.port.in.SubscriptionHistoryQueryUseCase;
import com.artinus.subscription.application.result.SubscriptionHistoryResult;
import com.artinus.subscription.application.result.SubscriptionResult;
import com.artinus.subscription.domain.subscription.SubscriptionActionType;
import com.artinus.subscription.domain.subscription.SubscriptionStatus;

@WebMvcTest(SubscriptionCommandController.class)
@Import(SubscriptionCommandController.class)
class SubscriptionCommandControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private SubscriptionCommandUseCase subscriptionCommandUseCase;

	@MockitoBean
	private SubscriptionHistoryQueryUseCase subscriptionHistoryQueryUseCase;

	@SpringBootConfiguration
	@EnableAutoConfiguration
	static class TestApplication {
	}

	@Test
	void subscribesMember() throws Exception {
		// given
		when(subscriptionCommandUseCase.subscribe(any()))
			.thenReturn(new SubscriptionResult("01012345678", SubscriptionStatus.BASIC));

		// when & then
		mockMvc.perform(post("/api/v1/subscriptions")
				.header("Idempotency-Key", "request-key")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "phoneNumber": "010-1234-5678",
					  "channelId": 1,
					  "targetStatus": "BASIC"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.phoneNumber").value("01012345678"))
			.andExpect(jsonPath("$.subscriptionStatus").value("BASIC"));
	}

	@Test
	void cancelsMemberSubscription() throws Exception {
		// given
		when(subscriptionCommandUseCase.cancel(any()))
			.thenReturn(new SubscriptionResult("01012345678", SubscriptionStatus.NONE));

		// when & then
		mockMvc.perform(post("/api/v1/subscriptions/cancel")
				.header("Idempotency-Key", "request-key")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "phoneNumber": "01012345678",
					  "channelId": 5,
					  "targetStatus": "NONE"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.phoneNumber").value("01012345678"))
			.andExpect(jsonPath("$.subscriptionStatus").value("NONE"));
	}

	@Test
	void findsSubscriptionHistories() throws Exception {
		// given
		when(subscriptionHistoryQueryUseCase.findByPhoneNumber("010-1234-5678"))
			.thenReturn(new SubscriptionHistoryResult(
				List.of(new SubscriptionHistoryResult.HistoryItem(
					"홈페이지",
					SubscriptionActionType.SUBSCRIBE,
					SubscriptionStatus.NONE,
					SubscriptionStatus.BASIC,
					LocalDateTime.of(2026, 1, 1, 10, 0)
				)),
				"2026년 1월 1일 홈페이지를 통해 일반 구독으로 구독하였습니다."
			));

		// when & then
		mockMvc.perform(get("/api/v1/subscriptions/histories")
				.param("phoneNumber", "010-1234-5678"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.history[0].channelName").value("홈페이지"))
			.andExpect(jsonPath("$.history[0].actionType").value("SUBSCRIBE"))
			.andExpect(jsonPath("$.history[0].afterStatus").value("BASIC"))
			.andExpect(jsonPath("$.summary").value("2026년 1월 1일 홈페이지를 통해 일반 구독으로 구독하였습니다."));
	}
}
