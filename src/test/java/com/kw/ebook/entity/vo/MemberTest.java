package com.kw.ebook.entity.vo;

import com.kw.ebook.exception.DomainException;
import com.kw.ebook.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Member 借閱上限規則")
class MemberTest {

    @Test
    void 未達上限時允許繼續借閱() {
        Member member = new Member(1L, "Alice", 5);
        assertDoesNotThrow(() -> member.ensureAllowsMoreLoans(4));
    }

    @Test
    void 已達上限時拒絕借閱() {
        Member member = new Member(1L, "Alice", 5);
        DomainException ex = assertThrows(DomainException.class,
                () -> member.ensureAllowsMoreLoans(5));
        assertEquals(ErrorCode.BORROW_LIMIT_EXCEEDED, ex.getErrorCode());
    }

    @Test
    void 上限不可為負數() {
        assertThrows(IllegalArgumentException.class, () -> new Member(1L, "Alice", -1));
    }
}
