package com.kw.ebook.service;

import com.kw.ebook.entity.vo.Loan;
import com.kw.ebook.enums.LoanStatus;

import java.util.List;
import java.util.Optional;

public interface LoanService {

    Loan save(Loan loan);

    Optional<Loan> findById(Long id);

    /** 某會員指定狀態的借閱單，依借閱時間倒序。 */
    List<Loan> findByMemberIdAndStatus(Long memberId, LoanStatus status);

    /** 某會員對某本書是否已有未歸還的借閱（防止重複借閱）。 */
    Optional<Loan> findActiveByMemberIdAndBookId(Long memberId, Long bookId);
}
