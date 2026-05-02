package com.artinus.subscription.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.artinus.subscription.application.FallbackHistorySummaryService;
import com.artinus.subscription.application.HistorySummaryPort;
import com.artinus.subscription.application.SubscriptionCommandService;
import com.artinus.subscription.application.SubscriptionHistoryQueryService;
import com.artinus.subscription.application.port.ChannelPort;
import com.artinus.subscription.application.port.ExternalApprovalPort;
import com.artinus.subscription.application.port.IdempotencyPort;
import com.artinus.subscription.application.port.MemberPort;
import com.artinus.subscription.application.port.SubscriptionHistoryPort;

@Configuration
public class ApplicationServiceConfig {

	@Bean
	public SubscriptionCommandService subscriptionCommandService(MemberPort memberPort, ChannelPort channelPort,
		SubscriptionHistoryPort historyPort, ExternalApprovalPort externalApprovalPort, IdempotencyPort idempotencyPort) {
		return new SubscriptionCommandService(memberPort, channelPort, historyPort, externalApprovalPort,
			idempotencyPort);
	}

	@Bean
	public SubscriptionHistoryQueryService subscriptionHistoryQueryService(SubscriptionHistoryPort historyPort,
		HistorySummaryPort summaryPort) {
		return new SubscriptionHistoryQueryService(historyPort, summaryPort);
	}

	@Bean
	public HistorySummaryPort historySummaryPort() {
		return new FallbackHistorySummaryService();
	}
}
