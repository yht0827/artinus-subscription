package com.artinus.subscription.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "llm")
public record LlmProperties(
	boolean enabled,
	String apiKey,
	String model,
	int maxOutputTokens
) {

	public boolean canUseOpenAi() {
		return enabled && apiKey != null && !apiKey.isBlank();
	}
}
