package com.artinus.subscription.api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class SubscriptionApiDocumentationConfig {

	@Bean
	public OpenAPI subscriptionOpenAPI() {
		return new OpenAPI()
			.info(new Info()
				.title("ARTINUS 구독 API")
				.description("구독 신청, 구독 해지, 구독 이력 조회 API 문서")
				.version("v1"));
	}
}
