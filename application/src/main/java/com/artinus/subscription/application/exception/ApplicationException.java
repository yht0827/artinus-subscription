package com.artinus.subscription.application.exception;

public abstract class ApplicationException extends RuntimeException {

	protected ApplicationException(String message) {
		super(message);
	}

	protected ApplicationException(String message, Throwable cause) {
		super(message, cause);
	}
}
