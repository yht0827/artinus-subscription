package com.artinus.subscription.infrastructure.exception;

public class IdempotencyResponseMappingException extends InfrastructureException {

	public IdempotencyResponseMappingException(String message, Throwable cause) {
		super(message, cause);
	}
}
