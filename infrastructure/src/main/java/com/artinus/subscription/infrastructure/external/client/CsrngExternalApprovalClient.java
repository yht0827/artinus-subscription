package com.artinus.subscription.infrastructure.external.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.artinus.subscription.application.port.out.ExternalApprovalPort;

@Component
public class CsrngExternalApprovalClient implements ExternalApprovalPort {

	private final RestClient restClient;
	private final CsrngApprovalPolicy approvalPolicy = new CsrngApprovalPolicy();
	private final String url;

	public CsrngExternalApprovalClient(RestClient.Builder restClientBuilder,
		@Value("${external.csrng.url}") String url) {
		this.restClient = restClientBuilder.build();
		this.url = url;
	}

	@Override
	public boolean approve() {
		return approvalPolicy.isApproved(requestRandomResponses());
	}

	private CsrngResponse[] requestRandomResponses() {
		return restClient.get()
			.uri(url)
			.retrieve()
			.body(CsrngResponse[].class);
	}

}
