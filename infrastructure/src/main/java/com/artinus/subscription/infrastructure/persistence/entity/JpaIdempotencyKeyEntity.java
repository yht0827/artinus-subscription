package com.artinus.subscription.infrastructure.persistence.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "idempotency_keys")
public class JpaIdempotencyKeyEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "phone_number", nullable = false, length = 20)
	private String phoneNumber;

	@Column(name = "idempotency_key", nullable = false, length = 100)
	private String idempotencyKey;

	@Column(name = "request_hash", nullable = false, length = 128)
	private String requestHash;

	@Column(name = "response_body", columnDefinition = "text")
	private String responseBody;

	@Column(name = "status_code")
	private Integer statusCode;

	@Enumerated(EnumType.STRING)
	@Column(name = "processing_status", nullable = false, length = 20)
	private IdempotencyProcessingStatus processingStatus;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "completed_at")
	private LocalDateTime completedAt;

	protected JpaIdempotencyKeyEntity() {
	}

	private JpaIdempotencyKeyEntity(String phoneNumber, String idempotencyKey, String requestHash, String responseBody,
		Integer statusCode) {
		LocalDateTime now = LocalDateTime.now();
		this.phoneNumber = phoneNumber;
		this.idempotencyKey = idempotencyKey;
		this.requestHash = requestHash;
		this.responseBody = responseBody;
		this.statusCode = statusCode;
		this.processingStatus = IdempotencyProcessingStatus.COMPLETED;
		this.createdAt = now;
		this.completedAt = now;
	}

	public static JpaIdempotencyKeyEntity completed(String phoneNumber, String idempotencyKey, String requestHash,
		String responseBody, int statusCode) {
		return new JpaIdempotencyKeyEntity(phoneNumber, idempotencyKey, requestHash, responseBody, statusCode);
	}

	public String getRequestHash() {
		return requestHash;
	}
}
