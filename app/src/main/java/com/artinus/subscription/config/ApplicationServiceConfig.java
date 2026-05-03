package com.artinus.subscription.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.artinus.subscription.application.idempotency.IdempotencyProcessor;
import com.artinus.subscription.application.port.out.ChannelPort;
import com.artinus.subscription.application.port.out.ExternalApprovalPort;
import com.artinus.subscription.application.port.out.HistorySummaryPort;
import com.artinus.subscription.application.port.out.IdempotencyPort;
import com.artinus.subscription.application.port.out.MemberPort;
import com.artinus.subscription.application.port.out.SubscriptionHistoryPort;
import com.artinus.subscription.application.service.SubscriptionCommandService;
import com.artinus.subscription.application.service.SubscriptionHistoryQueryService;
import com.artinus.subscription.application.summary.FallbackHistorySummaryService;

@Configuration
public class ApplicationServiceConfig {

	@Bean
	public SubscriptionCommandService subscriptionCommandService(MemberPort memberPort, ChannelPort channelPort,
		SubscriptionHistoryPort historyPort, ExternalApprovalPort externalApprovalPort,
		IdempotencyProcessor idempotencyProcessor) {
		return new SubscriptionCommandService(memberPort, channelPort, historyPort, externalApprovalPort,
			idempotencyProcessor);
	}

	@Bean
	public IdempotencyProcessor idempotencyProcessor(IdempotencyPort idempotencyPort) {
		return new IdempotencyProcessor(idempotencyPort);
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
