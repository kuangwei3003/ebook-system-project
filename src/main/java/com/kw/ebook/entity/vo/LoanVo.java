package com.kw.ebook.entity.vo;

import com.kw.ebook.enums.LoanStatus;

import java.time.Instant;

/**
 * 借閱單回應。
 *
 * <p>額外包含 bookTitle 以利前端顯示（讀取模型），
 * 由 LoanService 組裝；此為小規模應用下的務實取捨。</p>
 */
public record LoanVo(
        Long id,
        Long bookId,
        String bookTitle,
        Long memberId,
        LoanStatus status,
        Instant borrowedAt,
        Instant returnedAt) {

    public static LoanVo of(Loan loan, String bookTitle) {
        return new LoanVo(
                loan.getId(), loan.getBookId(), bookTitle, loan.getMemberId(),
                loan.getStatus(), loan.getBorrowedAt(), loan.getReturnedAt());
    }
}
