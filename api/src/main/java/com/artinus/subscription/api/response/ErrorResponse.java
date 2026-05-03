package com.artinus.subscription.api.response;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;

public record ErrorResponse(
	LocalDateTime timestamp,
	int status,
	String error,
	String message
) {

	public static ErrorResponse of(HttpStatus status, String message) {
		return new ErrorResponse(LocalDateTime.now(), status.value(), status.getReasonPhrase(), message);
	}
}
