package com.kw.ebook.exception;

/**
 * 領域錯誤碼：提供給 API 層作為穩定、可程式化處理的錯誤識別碼。
 */
public enum ErrorCode {
    BOOK_NOT_FOUND,
    MEMBER_NOT_FOUND,
    LOAN_NOT_FOUND,
    BOOK_UNAVAILABLE,
    BOOK_ALREADY_BORROWED,
    BORROW_LIMIT_EXCEEDED,
    LOAN_ALREADY_RETURNED
}
