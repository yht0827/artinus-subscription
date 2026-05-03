package com.artinus.subscription.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.artinus.subscription.api.response.ErrorResponse;
import com.artinus.subscription.application.exception.ApplicationException;
import com.artinus.subscription.application.exception.ChannelNotFoundException;
import com.artinus.subscription.application.exception.ExternalApprovalRejectedException;
import com.artinus.subscription.application.exception.IdempotencyConflictException;
import com.artinus.subscription.application.exception.MemberNotFoundException;
import com.artinus.subscription.domain.exception.DomainException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(DomainException.class)
	public ResponseEntity<ErrorResponse> handleDomainException(DomainException exception) {
		return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage());
	}

	@ExceptionHandler({MemberNotFoundException.class, ChannelNotFoundException.class})
	public ResponseEntity<ErrorResponse> handleNotFoundException(ApplicationException exception) {
		return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage());
	}

	@ExceptionHandler(IdempotencyConflictException.class)
	public ResponseEntity<ErrorResponse> handleIdempotencyConflictException(ApplicationException exception) {
		return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage());
	}

	@ExceptionHandler(ExternalApprovalRejectedException.class)
	public ResponseEntity<ErrorResponse> handleExternalApprovalRejectedException(ApplicationException exception) {
		return buildResponse(HttpStatus.BAD_GATEWAY, exception.getMessage());
	}

	@ExceptionHandler(ApplicationException.class)
	public ResponseEntity<ErrorResponse> handleApplicationException(ApplicationException exception) {
		return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage());
	}

	@ExceptionHandler(MissingRequestHeaderException.class)
	public ResponseEntity<ErrorResponse> handleMissingRequestHeaderException(MissingRequestHeaderException exception) {
		return buildResponse(HttpStatus.BAD_REQUEST, "%s 헤더는 필수입니다.".formatted(exception.getHeaderName()));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
		HttpMessageNotReadableException exception) {
		return buildResponse(HttpStatus.BAD_REQUEST, "요청 본문 형식이 올바르지 않습니다.");
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception exception) {
		log.error("처리되지 않은 예외가 발생했습니다.", exception);
		return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "요청을 처리하는 중 오류가 발생했습니다.");
	}

	private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message) {
		return ResponseEntity.status(status)
			.body(ErrorResponse.of(status, message));
	}
}
