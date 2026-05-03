package com.artinus.subscription.infrastructure.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.artinus.subscription.infrastructure.persistence.entity.JpaChannelEntity;

public interface ChannelRepository extends JpaRepository<JpaChannelEntity, Long> {

	Optional<JpaChannelEntity> findByName(String name);
}
