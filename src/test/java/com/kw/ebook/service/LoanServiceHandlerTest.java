package com.kw.ebook.service;

import com.kw.ebook.entity.vo.*;
import com.kw.ebook.enums.LoanStatus;
import com.kw.ebook.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("LoanService 借閱用例")
class LoanServiceHandlerTest {

    private static final Instant NOW = Instant.parse("2026-10-13T08:00:00Z");

    @Mock
    private BookService bookService;
    @Mock
    private MemberService memberService;
    @Mock
    private LoanService loanService;

    private LoanServiceHandler loanServiceHandler;

    @BeforeEach
    void setUp() {
        loanServiceHandler = new LoanServiceHandler(bookService, memberService, loanService,
                Clock.fixed(NOW, ZoneOffset.UTC));

        Member alice = new Member(1L, "Alice", 5);
        Book book = new Book(2L, "978-x", "Clean Architecture", "Robert C. Martin", "", 2, 2);

        when(memberService.findById(1L)).thenReturn(Optional.of(alice));
        when(bookService.findById(2L)).thenReturn(Optional.of(book));
        when(loanService.findActiveByMemberIdAndBookId(anyLong(), anyLong()))
                .thenReturn(Optional.empty());
        when(loanService.findByMemberIdAndStatus(1L, LoanStatus.ACTIVE))
                .thenReturn(List.of());
        when(loanService.save(any(Loan.class))).thenAnswer(invocation -> {
            Loan input = invocation.getArgument(0);
            return new Loan(100L, input.getBookId(), input.getMemberId(), input.getBorrowedAt());
        });
    }

    @Test
    void testBorrowBookSuccess() {
        LoanVo response = loanServiceHandler.borrowBook(1L, 2L);

        assertEquals(100L, response.id());
        assertEquals(2L, response.bookId());
        assertEquals("Clean Architecture", response.bookTitle());
        assertEquals(LoanStatus.ACTIVE, response.status());
        assertEquals(NOW, response.borrowedAt());
        assertEquals(1, bookService.findById(2L).orElseThrow().getAvailableCopies());
        verify(loanService).save(any(Loan.class));
    }

    @Test
    void testMemberNotFound() {
        assertDomainError(ErrorCode.MEMBER_NOT_FOUND, () -> loanServiceHandler.borrowBook(999L, 2L));
    }

    @Test
    void testBookNotFound() {
        assertDomainError(ErrorCode.BOOK_NOT_FOUND, () -> loanServiceHandler.borrowBook(1L, 999L));
    }

    @Test
    void testBookAlreadyBorrowed() {
        when(loanService.findActiveByMemberIdAndBookId(1L, 2L))
                .thenReturn(Optional.of(new Loan(50L, 2L, 1L, NOW)));

        assertDomainError(ErrorCode.BOOK_ALREADY_BORROWED, () -> loanServiceHandler.borrowBook(1L, 2L));
        verify(loanService, never()).save(any());
    }

    @Test
    void testBorrowLimitExceeded() {
        List<Loan> fiveActiveLoans = List.of(
                new Loan(1L, 10L, 1L, NOW), new Loan(2L, 11L, 1L, NOW),
                new Loan(3L, 12L, 1L, NOW), new Loan(4L, 13L, 1L, NOW),
                new Loan(5L, 14L, 1L, NOW));
        when(loanService.findByMemberIdAndStatus(1L, LoanStatus.ACTIVE)).thenReturn(fiveActiveLoans);

        assertDomainError(ErrorCode.BORROW_LIMIT_EXCEEDED, () -> loanServiceHandler.borrowBook(1L, 2L));
        verify(loanService, never()).save(any());
    }

    @Test
    void testBorrowBookWhenSoldOut() {
        Book soldOut = new Book(3L, "978-y", "Out of Stock", "Author", "", 1, 0);
                when(bookService.findById(3L)).thenReturn(Optional.of(soldOut));

        assertDomainError(ErrorCode.BOOK_UNAVAILABLE, () -> loanServiceHandler.borrowBook(1L, 3L));
        verify(loanService, never()).save(any());
    }

    @Test
    void testReturnLoanSuccess() {
        Book book = bookService.findById(2L).orElseThrow();
        book.borrowOneCopy(); // 先讓一本處於借出狀態，庫存 2 -> 1

        Loan loan = new Loan(100L, 2L, 1L, NOW);
        when(loanService.findById(100L)).thenReturn(Optional.of(loan));

        LoanVo response = loanServiceHandler.returnLoan(100L);

        assertEquals(LoanStatus.RETURNED, response.status());
        assertEquals(NOW, response.returnedAt());
        assertEquals(2, book.getAvailableCopies()); // 放回後恢復為 2
        verify(loanService).save(loan);
    }

    @Test
    void testReturnNonExistentLoan() {
        assertDomainError(ErrorCode.LOAN_NOT_FOUND, () -> loanServiceHandler.returnLoan(999L));
    }

    @Test
    void testReturnAlreadyReturnedLoan () {
        Loan loan = new Loan(100L, 2L, 1L, NOW);
        loan.markReturned(NOW);
        when(loanService.findById(100L)).thenReturn(Optional.of(loan));

        assertDomainError(ErrorCode.LOAN_ALREADY_RETURNED, () -> loanServiceHandler.returnLoan(100L));
    }

    /** 單一例外類型＋errorCode 區分後，斷言需同時驗證例外型別與錯誤碼。 */
    private void assertDomainError(ErrorCode expected, Executable executable) {
        DomainException ex = assertThrows(DomainException.class, executable);
        assertEquals(expected, ex.getErrorCode());
    }
}
