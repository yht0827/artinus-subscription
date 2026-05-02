package com.artinus.subscription.infrastructure.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.artinus.subscription.infrastructure.persistence.entity.JpaIdempotencyKeyEntity;

public interface IdempotencyKeyRepository extends JpaRepository<JpaIdempotencyKeyEntity, Long> {

	Optional<JpaIdempotencyKeyEntity> findByPhoneNumberAndIdempotencyKey(String phoneNumber, String idempotencyKey);
}
