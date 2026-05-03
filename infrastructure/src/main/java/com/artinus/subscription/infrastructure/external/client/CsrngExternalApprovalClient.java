package com.artinus.subscription.infrastructure.external.client;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.artinus.subscription.application.port.out.ExternalApprovalPort;
import com.artinus.subscription.infrastructure.resilience.ExternalApiResilience;

@Component
public class CsrngExternalApprovalClient implements ExternalApprovalPort {

	private final RestClient restClient;
	private final CsrngApprovalPolicy approvalPolicy = new CsrngApprovalPolicy();
	private final ExternalApiResilience resilience;
	private final String url;

	public CsrngExternalApprovalClient(RestClient.Builder restClientBuilder,
		@Value("${external.csrng.url}") String url,
		@Value("${external.csrng.connect-timeout}") Duration connectTimeout,
		@Value("${external.csrng.read-timeout}") Duration readTimeout) {
		this.restClient = restClientBuilder
			.requestFactory(requestFactory(connectTimeout, readTimeout))
			.build();
		this.resilience = ExternalApiResilience.create("csrng");
		this.url = url;
	}

	CsrngExternalApprovalClient(RestClient restClient, String url) {
		this.restClient = restClient;
		this.resilience = ExternalApiResilience.create("csrng");
		this.url = url;
	}

	@Override
	public boolean approve() {
		return approvalPolicy.isApproved(requestRandomResponses());
	}

	private CsrngResponse[] requestRandomResponses() {
		return resilience.execute(() -> restClient.get()
			.uri(url)
			.retrieve()
			.body(CsrngResponse[].class));
	}

	private SimpleClientHttpRequestFactory requestFactory(Duration connectTimeout, Duration readTimeout) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(connectTimeout);
		requestFactory.setReadTimeout(readTimeout);
		return requestFactory;
	}
}
