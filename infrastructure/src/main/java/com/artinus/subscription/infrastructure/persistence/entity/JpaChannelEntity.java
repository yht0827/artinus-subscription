package com.artinus.subscription.infrastructure.persistence.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "channels")
public class JpaChannelEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 50)
	private String name;

	@Column(name = "subscribe_enabled", nullable = false)
	private boolean subscribeEnabled;

	@Column(name = "cancel_enabled", nullable = false)
	private boolean cancelEnabled;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	protected JpaChannelEntity() {
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public boolean supportsSubscribe() {
		return subscribeEnabled;
	}

	public boolean supportsCancel() {
		return cancelEnabled;
	}
}
