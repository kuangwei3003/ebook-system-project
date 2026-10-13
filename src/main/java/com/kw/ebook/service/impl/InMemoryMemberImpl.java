package com.kw.ebook.service.impl;

import com.kw.ebook.entity.vo.Member;
import com.kw.ebook.service.MemberService;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryMemberImpl implements MemberService {

    private final Map<Long, Member> store = new ConcurrentHashMap<>();

    @Override
    public Optional<Member> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Member save(Member member) {
        store.put(member.getId(), member);
        return member;
    }
}
