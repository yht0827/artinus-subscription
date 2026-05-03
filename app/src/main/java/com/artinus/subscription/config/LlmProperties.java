package com.artinus.subscription.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "llm")
public record LlmProperties(
	boolean enabled,
	String apiKey,
	String model,
	int maxOutputTokens,
	Duration connectTimeout,
	Duration readTimeout
) {

	public boolean canUseOpenAi() {
		return enabled && apiKey != null && !apiKey.isBlank();
	}
}
