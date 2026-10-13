package com.kw.ebook.entity.vo;

import com.kw.ebook.enums.LoanStatus;
import com.kw.ebook.exception.DomainException;
import com.kw.ebook.exception.ErrorCode;
import java.time.Instant;

/**
 * 借閱單（Loan）：記錄「哪個會員借了哪本書」以及其生命週期。
 *
 * <p>狀態轉換由實體自身守衛：僅能 ACTIVE -> RETURNED 一次，
 * 重複歸還將拋出 DomainException（ErrorCode.LOAN_ALREADY_RETURNED）。</p>
 */
public class Loan {

    private final Long id;
    private final Long bookId;
    private final Long memberId;
    private final Instant borrowedAt;

    private LoanStatus status;
    private Instant returnedAt;

    /** 建立一筆新的借閱紀錄（建立即為 ACTIVE）。 */
    public Loan(Long id, Long bookId, Long memberId, Instant borrowedAt) {
        this.id = id;
        this.bookId = bookId;
        this.memberId = memberId;
        this.borrowedAt = borrowedAt;
        this.status = LoanStatus.ACTIVE;
    }

    /**
     * 標記為已歸還。
     *
     * @throws DomainException ErrorCode.LOAN_ALREADY_RETURNED — 已歸還過的借閱單不可重複歸還
     */
    public void markReturned(Instant returnedAt) {
        if (status == LoanStatus.RETURNED) {
            throw new DomainException(ErrorCode.LOAN_ALREADY_RETURNED,
                    "借閱單已歸還，不可重複歸還: id=" + id);
        }
        this.status = LoanStatus.RETURNED;
        this.returnedAt = returnedAt;
    }

    public boolean isActive() {
        return status == LoanStatus.ACTIVE;
    }

    public Long getId() {
        return id;
    }

    public Long getBookId() {
        return bookId;
    }

    public Long getMemberId() {
        return memberId;
    }

    public Instant getBorrowedAt() {
        return borrowedAt;
    }

    public LoanStatus getStatus() {
        return status;
    }

    public Instant getReturnedAt() {
        return returnedAt;
    }
}
