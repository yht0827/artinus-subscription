package com.artinus.subscription.infrastructure.external.client;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class CsrngExternalApprovalClientTest {

	private static final String CSRNG_URL = "https://csrng.net/csrng/csrng.php?min=0&max=1";

	private MockRestServiceServer server;
	private CsrngExternalApprovalClient client;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		client = new CsrngExternalApprovalClient(builder, CSRNG_URL);
	}

	@Test
	void approvesWhenCsrngRandomValueIsOne() {
		// given
		server.expect(requestTo(CSRNG_URL))
			.andRespond(withSuccess("[{\"status\":\"success\",\"min\":0,\"max\":1,\"random\":1}]",
				MediaType.APPLICATION_JSON));

		// when & then
		assertThat(client.approve()).isTrue();
		server.verify();
	}

	@Test
	void rejectsWhenCsrngRandomValueIsZero() {
		// given
		server.expect(requestTo(CSRNG_URL))
			.andRespond(withSuccess("[{\"status\":\"success\",\"min\":0,\"max\":1,\"random\":0}]",
				MediaType.APPLICATION_JSON));

		// when & then
		assertThat(client.approve()).isFalse();
		server.verify();
	}

	@Test
	void rejectsWhenCsrngResponseIsEmpty() {
		// given
		server.expect(requestTo(CSRNG_URL))
			.andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

		// when & then
		assertThat(client.approve()).isFalse();
		server.verify();
	}
}
