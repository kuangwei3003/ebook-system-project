package com.kw.ebook.exception;

/**
 * 領域業務例外：所有業務規則違反共用此單一例外類型，
 * 依 {@link ErrorCode} 區分（不再以子類別划分），
 * 呼叫端（如 API 層）據此映射 HTTP 狀態與機械可讀錯誤碼。
 *
 * <p>繼承 RuntimeException，讓事務可自然回滾。</p>
 */
public class DomainException extends RuntimeException {

    private final ErrorCode errorCode;

    public DomainException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
