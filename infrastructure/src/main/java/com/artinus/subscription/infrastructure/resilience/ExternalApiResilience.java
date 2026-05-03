package com.artinus.subscription.infrastructure.resilience;

import java.time.Duration;
import java.util.function.Supplier;

import org.springframework.web.client.RestClientException;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;

public class ExternalApiResilience {

	private final CircuitBreaker circuitBreaker;
	private final Retry retry;

	private ExternalApiResilience(CircuitBreaker circuitBreaker, Retry retry) {
		this.circuitBreaker = circuitBreaker;
		this.retry = retry;
	}

	public static ExternalApiResilience create(String name) {
		CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
			.failureRateThreshold(50)
			.slidingWindowSize(10)
			.minimumNumberOfCalls(5)
			.waitDurationInOpenState(Duration.ofSeconds(10))
			.permittedNumberOfCallsInHalfOpenState(2)
			.recordExceptions(RestClientException.class)
			.build();
		RetryConfig retryConfig = RetryConfig.custom()
			.maxAttempts(2)
			.waitDuration(Duration.ofMillis(200))
			.retryExceptions(RestClientException.class)
			.build();
		return new ExternalApiResilience(
			CircuitBreaker.of(name, circuitBreakerConfig),
			Retry.of(name, retryConfig)
		);
	}

	public <T> T execute(Supplier<T> supplier) {
		Supplier<T> decorated = CircuitBreaker.decorateSupplier(circuitBreaker, supplier);
		return Retry.decorateSupplier(retry, decorated).get();
	}
}
