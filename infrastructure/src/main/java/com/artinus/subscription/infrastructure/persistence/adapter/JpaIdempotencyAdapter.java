package com.artinus.subscription.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.artinus.subscription.application.port.IdempotencyPort;
import com.artinus.subscription.application.result.CompletedIdempotency;
import com.artinus.subscription.application.result.SubscriptionResult;
import com.artinus.subscription.infrastructure.persistence.entity.JpaIdempotencyKeyEntity;
import com.artinus.subscription.infrastructure.persistence.repository.IdempotencyKeyRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Repository
public class JpaIdempotencyAdapter implements IdempotencyPort {

	private static final int SUCCESS_STATUS_CODE = 200;

	private final IdempotencyKeyRepository idempotencyKeyRepository;
	private final ObjectMapper objectMapper;

	public JpaIdempotencyAdapter(IdempotencyKeyRepository idempotencyKeyRepository, ObjectMapper objectMapper) {
		this.idempotencyKeyRepository = idempotencyKeyRepository;
		this.objectMapper = objectMapper;
	}

	@Override
	public Optional<CompletedIdempotency> findCompleted(String phoneNumber, String idempotencyKey) {
		return idempotencyKeyRepository.findByPhoneNumberAndIdempotencyKey(phoneNumber, idempotencyKey)
			.map(this::toCompletedIdempotency);
	}

	@Override
	public void saveCompleted(CompletedIdempotency completed) {
		idempotencyKeyRepository.save(JpaIdempotencyKeyEntity.completed(
			completed.phoneNumber(),
			completed.idempotencyKey(),
			completed.requestHash(),
			writeResponse(completed.response()),
			SUCCESS_STATUS_CODE
		));
	}

	private CompletedIdempotency toCompletedIdempotency(JpaIdempotencyKeyEntity entity) {
		return new CompletedIdempotency(
			entity.getPhoneNumber(),
			entity.getIdempotencyKey(),
			entity.getRequestHash(),
			readResponse(entity.getResponseBody())
		);
	}

	private String writeResponse(SubscriptionResult response) {
		try {
			return objectMapper.writeValueAsString(response);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("Failed to serialize idempotency response.", exception);
		}
	}

	private SubscriptionResult readResponse(String responseBody) {
		try {
			return objectMapper.readValue(responseBody, SubscriptionResult.class);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("Failed to deserialize idempotency response.", exception);
		}
	}
}
