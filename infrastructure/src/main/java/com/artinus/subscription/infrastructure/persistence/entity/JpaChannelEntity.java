package com.artinus.subscription.infrastructure.persistence.entity;

import java.time.LocalDateTime;

import com.artinus.subscription.domain.channel.Channel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "channels")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

	public boolean supportsSubscribe() {
		return subscribeEnabled;
	}

	public boolean supportsCancel() {
		return cancelEnabled;
	}

	public Channel toDomain() {
		return new Channel(id, name, subscribeEnabled, cancelEnabled);
	}
}
