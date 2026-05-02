package com.artinus.subscription.infrastructure.persistence.entity;

import java.time.LocalDateTime;

import com.artinus.subscription.domain.subscription.SubscriptionActionType;
import com.artinus.subscription.domain.subscription.SubscriptionStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "subscription_histories")
public class JpaSubscriptionHistoryEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_id", nullable = false)
	private JpaMemberEntity member;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "channel_id", nullable = false)
	private JpaChannelEntity channel;

	@Enumerated(EnumType.STRING)
	@Column(name = "action_type", nullable = false, length = 20)
	private SubscriptionActionType actionType;

	@Enumerated(EnumType.STRING)
	@Column(name = "before_status", nullable = false, length = 20)
	private SubscriptionStatus beforeStatus;

	@Enumerated(EnumType.STRING)
	@Column(name = "after_status", nullable = false, length = 20)
	private SubscriptionStatus afterStatus;

	@Column(name = "changed_at", nullable = false)
	private LocalDateTime changedAt;

	protected JpaSubscriptionHistoryEntity() {
	}

	private JpaSubscriptionHistoryEntity(JpaMemberEntity member, JpaChannelEntity channel, SubscriptionActionType actionType,
		SubscriptionStatus beforeStatus, SubscriptionStatus afterStatus, LocalDateTime changedAt) {
		this.member = member;
		this.channel = channel;
		this.actionType = actionType;
		this.beforeStatus = beforeStatus;
		this.afterStatus = afterStatus;
		this.changedAt = changedAt;
	}

	public static JpaSubscriptionHistoryEntity record(JpaMemberEntity member, JpaChannelEntity channel, SubscriptionActionType actionType,
		SubscriptionStatus beforeStatus, SubscriptionStatus afterStatus, LocalDateTime changedAt) {
		return new JpaSubscriptionHistoryEntity(member, channel, actionType, beforeStatus, afterStatus, changedAt);
	}

	public SubscriptionActionType getActionType() {
		return actionType;
	}

	public JpaMemberEntity getMember() {
		return member;
	}

	public JpaChannelEntity getChannel() {
		return channel;
	}

	public SubscriptionStatus getBeforeStatus() {
		return beforeStatus;
	}

	public SubscriptionStatus getAfterStatus() {
		return afterStatus;
	}

	public LocalDateTime getChangedAt() {
		return changedAt;
	}
}
