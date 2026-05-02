package com.artinus.subscription.application;

public class ExternalApprovalRejectedException extends RuntimeException {

	public ExternalApprovalRejectedException() {
		super("External approval rejected the request.");
	}
}
