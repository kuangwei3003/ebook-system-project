package com.kw.ebook.service;

import com.kw.ebook.entity.vo.*;
import com.kw.ebook.enums.LoanStatus;
import com.kw.ebook.exception.DomainException;
import com.kw.ebook.exception.ErrorCode;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 借閱應用服務：編排借閱／歸還／查詢用例。
 *
 * <p>規則由領域物件守衛（Book 守庫存、Member 守上限、Loan 守狀態機），
 * 本服務只負責取用聚合、檢查前置條件與持久化。</p>
 *
 * <p>並發取捨：庫存扣減由 Book 的同步方法保證不會超借；
 * 「重複借閱檢查 → 建立借閱單」非跨聚合原子操作，極端並發下可能產生
 * 同會員同書之重複借閱。若升級為 DB 實作，應以（memberId, bookId）
 * 上「未歸還借閱」之唯一條件索引來徹底防堵。</p>
 */
@Service
public class LoanServiceHandler {

    private static final Logger log = LoggerFactory.getLogger(LoanServiceHandler.class);

    @Resource
    private  BookService bookService;
    @Resource
    private  MemberService memberService;
    @Resource
    private  LoanService loanService;
    @Resource
    private  Clock clock;

    public LoanServiceHandler(BookService bookService, MemberService memberService, LoanService loanService, Clock clock) {
        this.bookService = bookService;
        this.memberService = memberService;
        this.loanService = loanService;
        this.clock = clock;
    }


    /**
     * 借閱書籍。
     *
     * 前置檢查順序刻意由「成本低、訊息明確」至「庫存扣減」：
     * 會員存在 → 書籍存在 → 未重複借閱 → 未達借閱上限 → 扣減庫存並建立借閱單。
     */
    public LoanVo borrowBook(Long memberId, Long bookId) {
        Member member = memberService.findById(memberId)
                .orElseThrow(() -> new DomainException(
                        ErrorCode.MEMBER_NOT_FOUND, "會員不存在: id=" + memberId));
        Book book = bookService.findById(bookId)
                .orElseThrow(() -> new DomainException(
                        ErrorCode.BOOK_NOT_FOUND, "書籍不存在: id=" + bookId));

        loanService.findActiveByMemberIdAndBookId(memberId, bookId).ifPresent(existing -> {
            throw new DomainException(ErrorCode.BOOK_ALREADY_BORROWED,
                    "會員已借閱該書籍且尚未歸還: memberId=" + memberId + ", bookId=" + bookId);
        });

        long activeCount = loanService.findByMemberIdAndStatus(memberId, LoanStatus.ACTIVE).size();
        member.ensureAllowsMoreLoans(activeCount);

        book.borrowOneCopy(); // 無庫存時拋 BOOK_UNAVAILABLE
        Loan loan = loanService.save(new Loan(null, bookId, memberId, Instant.now(clock)));
        log.info("會員 {} 借閱書籍 {}，建立借閱單 {}", memberId, bookId, loan.getId());
        return LoanVo.of(loan, book.getTitle());
    }

    /** 歸還書籍：借閱單狀態機轉換（ACTIVE -> RETURNED）+ 庫存放回。 */
    public LoanVo returnLoan(Long loanId) {
        Loan loan = loanService.findById(loanId)
                .orElseThrow(() -> new DomainException(
                        ErrorCode.LOAN_NOT_FOUND, "借閱單不存在: id=" + loanId));

        loan.markReturned(Instant.now(clock)); // 已歸還時拋 LOAN_ALREADY_RETURNED
        loanService.save(loan);             // 顯式持久化狀態變更（記憶體實作下為冪等 put）
        Book book = bookService.findById(loan.getBookId())
                .orElseThrow(() -> new DomainException(
                        ErrorCode.BOOK_NOT_FOUND, "書籍不存在: id=" + loan.getBookId()));
        book.returnOneCopy();

        log.info("借閱單 {} 已歸還，書籍 {} 庫存放回", loanId, book.getId());
        return LoanVo.of(loan, book.getTitle());
    }

    /** 查詢會員的借閱單（預設查目前借閱中：ACTIVE）。 */
    public List<LoanVo> listLoans(Long memberId, LoanStatus status) {
        memberService.findById(memberId)
                .orElseThrow(() -> new DomainException(
                        ErrorCode.MEMBER_NOT_FOUND, "會員不存在: id=" + memberId));
        return loanService.findByMemberIdAndStatus(memberId, status).stream()
                .map(loan -> LoanVo.of(loan, resolveBookTitle(loan.getBookId())))
                .toList();
    }

    /** 依 id 查詢單筆借閱單。 */
    public LoanVo getLoan(Long loanId) {
        Loan loan = loanService.findById(loanId)
                .orElseThrow(() -> new DomainException(
                        ErrorCode.LOAN_NOT_FOUND, "借閱單不存在: id=" + loanId));
        return LoanVo.of(loan, resolveBookTitle(loan.getBookId()));
    }

    /** 借閱單必然對應一本書；資料不一致屬系統異常，此處以佔位標題防禦。 */
    private String resolveBookTitle(Long bookId) {
        return bookService.findById(bookId).map(Book::getTitle).orElse("(unknown book)");
    }
}
