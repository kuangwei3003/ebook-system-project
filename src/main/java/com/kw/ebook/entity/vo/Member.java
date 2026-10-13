package com.kw.ebook.entity.vo;

import com.kw.ebook.exception.DomainException;
import com.kw.ebook.exception.ErrorCode;

/**
 * 借閱會員。
 *
 * <p>「單一會員最多可同時借閱 maxActiveLoans 本書」的規則由本實體守衛。</p>
 */
public class Member {

    /** 系統預設的同時借閱上限。 */
    public static final int DEFAULT_MAX_ACTIVE_LOANS = 5;

    private final Long id;
    private final String name;
    private final int maxActiveLoans;

    public Member(Long id, String name, int maxActiveLoans) {
        if (maxActiveLoans < 0) {
            throw new IllegalArgumentException("借閱上限不可為負數");
        }
        this.id = id;
        this.name = name;
        this.maxActiveLoans = maxActiveLoans;
    }

    /**
     * 檢查在已借閱 currentActiveLoans 本的情況下，是否還允許再借一本。
     *
     * @throws DomainException ErrorCode.BORROW_LIMIT_EXCEEDED — 已達上限時
     */
    public void ensureAllowsMoreLoans(long currentActiveLoans) {
        if (currentActiveLoans >= maxActiveLoans) {
            throw new DomainException(ErrorCode.BORROW_LIMIT_EXCEEDED,
                    "已達借閱上限 (" + maxActiveLoans + " 本): memberId=" + id);
        }
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getMaxActiveLoans() {
        return maxActiveLoans;
    }
}
