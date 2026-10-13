package com.kw.ebook.service;

import com.kw.ebook.entity.vo.Member;
import java.util.Optional;

public interface MemberService {

    Optional<Member> findById(Long id);

    Member save(Member member);
}
