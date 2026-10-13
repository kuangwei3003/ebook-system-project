package com.kw.ebook.entity.req;

import jakarta.validation.constraints.NotNull;

/** 借閱請求：於特定書籍資源下提交，指出借閱者。 */
public record BorrowBookReq(
        @NotNull(message = "memberId 不可為空") Long memberId) {
}
