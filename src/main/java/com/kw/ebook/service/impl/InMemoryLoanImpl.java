package com.kw.ebook.service.impl;
import com.kw.ebook.entity.vo.Loan;

import com.kw.ebook.enums.LoanStatus;
import com.kw.ebook.service.LoanService;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryLoanImpl implements LoanService {

    private final Map<Long, Loan> store = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(0);

    @Override
    public Loan save(Loan loan) {
        // 新建借閱單（尚未指派 id）時由倉儲賦予遞增 id，並回傳帶 id 的實體
        if (loan.getId() == null) {
            Loan withId = new Loan(idSequence.incrementAndGet(),
                    loan.getBookId(), loan.getMemberId(), loan.getBorrowedAt());
            store.put(withId.getId(), withId);
            return withId;
        }
        store.put(loan.getId(), loan);
        return loan;
    }

    @Override
    public Optional<Loan> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Loan> findByMemberIdAndStatus(Long memberId, LoanStatus status) {
        return store.values().stream()
                .filter(loan -> loan.getMemberId().equals(memberId))
                .filter(loan -> loan.getStatus().equals(status))
                .sorted(Comparator.comparing(Loan::getBorrowedAt).reversed())
                .toList();
    }

    @Override
    public Optional<Loan> findActiveByMemberIdAndBookId(Long memberId, Long bookId) {
        return store.values().stream()
                .filter(loan -> loan.getMemberId().equals(memberId))
                .filter(loan -> loan.getBookId().equals(bookId))
                .filter(Loan::isActive)
                .findFirst();
    }
}
