package com.artinus.subscription.infrastructure.external;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.artinus.subscription.application.port.ExternalApprovalPort;

@Component
public class CsrngExternalApprovalClient implements ExternalApprovalPort {

	private final RestClient restClient;
	private final String url;

	@Autowired
	public CsrngExternalApprovalClient(RestClient.Builder restClientBuilder,
		@Value("${external.csrng.url}") String url) {
		this(restClientBuilder.build(), url);
	}

	CsrngExternalApprovalClient(RestClient restClient, String url) {
		this.restClient = restClient;
		this.url = url;
	}

	@Override
	public boolean approve() {
		CsrngResponse[] responses = restClient.get()
			.uri(url)
			.retrieve()
			.body(CsrngResponse[].class);

		return responses != null
			&& responses.length > 0
			&& responses[0].random() == 1;
	}

	private record CsrngResponse(String status, int min, int max, int random) {
	}
}
