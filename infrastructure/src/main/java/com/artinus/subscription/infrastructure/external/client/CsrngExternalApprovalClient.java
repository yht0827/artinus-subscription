package com.artinus.subscription.infrastructure.external.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.artinus.subscription.application.port.out.ExternalApprovalPort;

@Component
public class CsrngExternalApprovalClient implements ExternalApprovalPort {

	private static final int APPROVED_RANDOM_VALUE = 1;

	private final RestClient restClient;
	private final String url;

	public CsrngExternalApprovalClient(RestClient.Builder restClientBuilder,
		@Value("${external.csrng.url}") String url) {
		this.restClient = restClientBuilder.build();
		this.url = url;
	}

	@Override
	public boolean approve() {
		return hasApprovedResponse(requestRandomResponses());
	}

	private CsrngResponse[] requestRandomResponses() {
		return restClient.get()
			.uri(url)
			.retrieve()
			.body(CsrngResponse[].class);
	}

	private boolean hasApprovedResponse(CsrngResponse[] responses) {
		return responses != null
			&& responses.length > 0
			&& responses[0].random() == APPROVED_RANDOM_VALUE;
	}

	private record CsrngResponse(String status, int min, int max, int random) {
	}
}
