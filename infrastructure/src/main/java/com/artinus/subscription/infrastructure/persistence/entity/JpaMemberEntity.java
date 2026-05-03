package com.artinus.subscription.infrastructure.persistence.entity;

import java.time.LocalDateTime;

import com.artinus.subscription.domain.exception.RequiredSubscriptionStatusException;
import com.artinus.subscription.domain.member.Member;
import com.artinus.subscription.domain.member.PhoneNumber;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

	private JpaMemberEntity(String phoneNumber, SubscriptionStatus subscriptionStatus) {
		this.phoneNumber = PhoneNumber.from(phoneNumber).value();
		this.subscriptionStatus = subscriptionStatus;
	}

	public static JpaMemberEntity create(String phoneNumber, SubscriptionStatus subscriptionStatus) {
		if (subscriptionStatus == null) {
			throw new RequiredSubscriptionStatusException();
		}
		return new JpaMemberEntity(phoneNumber, subscriptionStatus);
	}

	public static JpaMemberEntity from(Member member) {
		return create(member.getPhoneNumber(), member.getSubscriptionStatus());
	}

	public void changeStatus(SubscriptionStatus subscriptionStatus) {
		if (subscriptionStatus == null) {
			throw new RequiredSubscriptionStatusException();
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
