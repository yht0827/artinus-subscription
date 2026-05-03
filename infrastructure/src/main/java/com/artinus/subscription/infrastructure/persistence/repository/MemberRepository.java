package com.artinus.subscription.infrastructure.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.artinus.subscription.infrastructure.persistence.entity.JpaMemberEntity;

public interface MemberRepository extends JpaRepository<JpaMemberEntity, Long> {

	Optional<JpaMemberEntity> findByPhoneNumber(String phoneNumber);
}
