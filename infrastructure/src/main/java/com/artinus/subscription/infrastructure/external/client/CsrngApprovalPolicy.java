package com.artinus.subscription.infrastructure.external.client;

public class CsrngApprovalPolicy {

	private static final int APPROVED_RANDOM_VALUE = 1;

	public boolean isApproved(CsrngResponse[] responses) {
		return responses != null
			&& responses.length > 0
			&& responses[0].random() == APPROVED_RANDOM_VALUE;
	}
}
