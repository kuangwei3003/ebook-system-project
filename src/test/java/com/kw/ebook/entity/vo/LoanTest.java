package com.kw.ebook.entity.vo;

import com.kw.ebook.enums.LoanStatus;
import com.kw.ebook.exception.DomainException;
import com.kw.ebook.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Loan 狀態機規則")
class LoanTest {

    @Test
    void testNewLoanStatus() {
        Loan loan = new Loan(1L, 2L, 3L, Instant.parse("2026-10-13T00:00:00Z"));
        assertEquals(LoanStatus.ACTIVE, loan.getStatus());
        assertTrue(loan.isActive());
        assertNull(loan.getReturnedAt());
    }

    @Test
    void testReturnLoanStatusAndTime() {
        Loan loan = new Loan(1L, 2L, 3L, Instant.parse("2026-10-13T00:00:00Z"));
        Instant returnedAt = Instant.parse("2026-10-20T08:30:00Z");
        loan.markReturned(returnedAt);
        assertEquals(LoanStatus.RETURNED, loan.getStatus());
        assertEquals(returnedAt, loan.getReturnedAt());
        assertFalse(loan.isActive());
    }

    @Test
    void testRejectDuplicateReturn() {
        Loan loan = new Loan(1L, 2L, 3L, Instant.parse("2026-10-13T00:00:00Z"));
        loan.markReturned(Instant.parse("2026-10-20T00:00:00Z"));
        DomainException ex = assertThrows(DomainException.class,
                () -> loan.markReturned(Instant.parse("2026-10-21T00:00:00Z")));
        assertEquals(ErrorCode.LOAN_ALREADY_RETURNED, ex.getErrorCode());
    }
}
