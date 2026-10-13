package com.kw.ebook.exception;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * 全域錯誤處理：把例外統一轉換為 RFC 7807 Problem Detail 回應。
 *
 * <p>原則：
 * 1. 業務規則違反（DomainException）→ 404/409，附穩定 errorCode；
 * 2. 輸入問題（驗證/格式）→ 400，標示欄位明細；
 * 3. 未預期例外 → 500，記錄日誌但不外洩內部細節。</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 領域例外 → 依錯誤碼語意映射 HTTP 狀態。 */
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ProblemDetail> handleDomain(DomainException ex) {
        HttpStatus status = switch (ex.getErrorCode()) {
            case BOOK_NOT_FOUND, MEMBER_NOT_FOUND, LOAN_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case BOOK_UNAVAILABLE, BOOK_ALREADY_BORROWED,
                 BORROW_LIMIT_EXCEEDED, LOAN_ALREADY_RETURNED -> HttpStatus.CONFLICT;
        };
        return problem(status, ex.getErrorCode().name(), ex.getMessage());
    }

    /** Request body 欄位驗證失敗 → 400，並列出各欄位錯誤。 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleBodyValidation(
            MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .sorted(Comparator.comparing(f -> f.getField()))
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return problem(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", detail);
    }

    /** Query / path 參數驗證失敗（@Validated）→ 400。 */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleParamViolation(
            ConstraintViolationException ex) {
        String detail = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        return problem(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", detail);
    }

    /** 參數型別/格式錯誤、請求體不可解析、服務層參數檢查 → 400。 */
    @ExceptionHandler({MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class,
            IllegalArgumentException.class})
    public ResponseEntity<ProblemDetail> handleMalformed(Exception ex) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "請求參數或請求體格式不正確");
    }

    /** 未對應到任何端點的路徑 → 404（維持 Problem Detail 格式一致性）。 */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> handleNoResource(
            NoResourceFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "要求的資源不存在");
    }

    /** 未預期例外（兜底）：記錄完整堆疊，僅回傳安全的一般性訊息。 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
        log.error("未預期的系統例外", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "系統錯誤，請稍後再試");
    }

    private ResponseEntity<ProblemDetail> problem(
            HttpStatus status, String code, String detail) {
        ProblemDetail pd =
                ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(status.getReasonPhrase());
        pd.setProperty("code", code);
        return ResponseEntity.status(status).body(pd);
    }
}
