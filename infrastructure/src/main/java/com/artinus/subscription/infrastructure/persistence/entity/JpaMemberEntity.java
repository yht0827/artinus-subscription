package com.artinus.subscription.infrastructure.persistence.entity;

import java.time.LocalDateTime;

import com.artinus.subscription.domain.member.PhoneNumber;
import com.artinus.subscription.domain.member.Member;
import com.artinus.subscription.domain.subscription.SubscriptionStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "members")
public class JpaMemberEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "phone_number", nullable = false, unique = true, length = 20)
	private String phoneNumber;

	@Enumerated(EnumType.STRING)
	@Column(name = "subscription_status", nullable = false, length = 20)
	private SubscriptionStatus subscriptionStatus;

	@Version
	@Column(nullable = false)
	private Long version;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	protected JpaMemberEntity() {
	}

	private JpaMemberEntity(String phoneNumber, SubscriptionStatus subscriptionStatus) {
		this.phoneNumber = PhoneNumber.from(phoneNumber).value();
		this.subscriptionStatus = subscriptionStatus;
	}

	public static JpaMemberEntity create(String phoneNumber, SubscriptionStatus subscriptionStatus) {
		if (subscriptionStatus == null) {
			throw new IllegalArgumentException("Subscription status is required.");
		}
		return new JpaMemberEntity(phoneNumber, subscriptionStatus);
	}

	public static JpaMemberEntity from(Member member) {
		return create(member.getPhoneNumber(), member.getSubscriptionStatus());
	}

	public Long getId() {
		return id;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public SubscriptionStatus getSubscriptionStatus() {
		return subscriptionStatus;
	}

	public void changeStatus(SubscriptionStatus subscriptionStatus) {
		if (subscriptionStatus == null) {
			throw new IllegalArgumentException("Subscription status is required.");
		}
		this.subscriptionStatus = subscriptionStatus;
	}

	public Member toDomain() {
		return Member.create(phoneNumber, subscriptionStatus);
	}

	@PrePersist
	void onCreate() {
		LocalDateTime now = LocalDateTime.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		this.updatedAt = LocalDateTime.now();
	}
}
