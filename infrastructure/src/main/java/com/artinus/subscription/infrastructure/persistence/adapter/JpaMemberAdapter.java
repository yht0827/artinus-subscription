package com.artinus.subscription.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.artinus.subscription.application.port.out.MemberPort;
import com.artinus.subscription.domain.member.Member;
import com.artinus.subscription.infrastructure.persistence.entity.JpaMemberEntity;
import com.artinus.subscription.infrastructure.persistence.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class JpaMemberAdapter implements MemberPort {

	private final MemberRepository memberRepository;

	@Override
	public Optional<Member> findByPhoneNumber(String phoneNumber) {
		return memberRepository.findByPhoneNumber(phoneNumber)
			.map(JpaMemberEntity::toDomain);
	}

	@Override
	public Member save(Member member) {
		JpaMemberEntity entity = memberRepository.findByPhoneNumber(member.getPhoneNumber())
			.orElseGet(() -> JpaMemberEntity.from(member));
		entity.changeStatus(member.getSubscriptionStatus());
		return memberRepository.save(entity).toDomain();
	}
}
