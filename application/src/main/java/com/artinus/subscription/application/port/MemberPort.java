package com.artinus.subscription.application.port;

import java.util.Optional;

import com.artinus.subscription.domain.member.Member;

public interface MemberPort {

	Optional<Member> findByPhoneNumber(String phoneNumber);

	Member save(Member member);
}
