package com.artinus.subscription.infrastructure.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.artinus.subscription.infrastructure.persistence.entity.JpaSubscriptionHistoryEntity;

public interface SubscriptionHistoryRepository extends JpaRepository<JpaSubscriptionHistoryEntity, Long> {

	List<JpaSubscriptionHistoryEntity> findByMemberIdOrderByChangedAtAsc(Long memberId);
}
