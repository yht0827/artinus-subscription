package com.artinus.subscription.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import com.artinus.subscription.application.SubscriptionCommandService;
import com.artinus.subscription.application.SubscriptionHistoryQueryService;
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
	private SubscriptionCommandService subscriptionCommandService;

	@MockitoBean
	private SubscriptionHistoryQueryService subscriptionHistoryQueryService;

	@SpringBootConfiguration
	@EnableAutoConfiguration
	static class TestApplication {
	}

	@Test
	void subscribesMember() throws Exception {
		when(subscriptionCommandService.subscribe(any()))
			.thenReturn(new SubscriptionResult("01012345678", SubscriptionStatus.BASIC));

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
		when(subscriptionCommandService.cancel(any()))
			.thenReturn(new SubscriptionResult("01012345678", SubscriptionStatus.NONE));

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
		when(subscriptionHistoryQueryService.findByPhoneNumber("010-1234-5678"))
			.thenReturn(new SubscriptionHistoryResult(
				List.of(new SubscriptionHistoryResult.HistoryItem(
					"홈페이지",
					SubscriptionActionType.SUBSCRIBE,
					SubscriptionStatus.NONE,
					SubscriptionStatus.BASIC,
					LocalDateTime.of(2026, 1, 1, 10, 0)
				)),
				""
			));

		mockMvc.perform(get("/api/v1/subscriptions/histories")
				.param("phoneNumber", "010-1234-5678"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.history[0].channelName").value("홈페이지"))
			.andExpect(jsonPath("$.history[0].actionType").value("SUBSCRIBE"))
			.andExpect(jsonPath("$.history[0].afterStatus").value("BASIC"))
			.andExpect(jsonPath("$.summary").value(""));
	}
}
