package com.kw.ebook.controller;

import com.kw.ebook.entity.req.BorrowBookReq;
import com.kw.ebook.entity.vo.*;
import com.kw.ebook.service.BookServiceHandler;
import com.kw.ebook.service.LoanServiceHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/**
 * 書籍相關 API：瀏覽、詳情、借閱。
 *
 * <p>資源導向設計：借閱是發生在「特定書籍」上的動作，
 * 故以 POST /api/books/{id}/borrow 表達；建立成功回 201 並帶 Location。</p>
 */
@Tag(name = "書籍", description = "書籍瀏覽、詳情與借閱")
@RestController
@RequestMapping("/api/books")
@Validated
public class BookController {

    @Resource
    private  BookServiceHandler bookServiceHandler;
    @Resource
    private  LoanServiceHandler loanServiceHandler;

    /** 瀏覽書籍：支援關鍵字模糊比對與分頁。 */
    @Operation(summary = "瀏覽書籍", description = "支援關鍵字模糊比對與分頁")
    @GetMapping
    public PageVo<PageVo.BookListItem> listBooks(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        PageResult<Book> result = bookServiceHandler.searchBooks(keyword, page, size);
        return new PageVo<>(
                result.items().stream().map(PageVo.BookListItem::from).toList(),
                result.page(), result.size(), result.totalElements(),
                result.totalPages(), result.hasNext());
    }

    /** 查詢書籍詳細資料。 */
    @Operation(summary = "查詢書籍詳情")
    @GetMapping("/{id}")
    public BookDetailVo getBookDetail(@PathVariable Long id) {
        return BookDetailVo.from(bookServiceHandler.getBookDetail(id));
    }

    /** 借閱書籍。 */
    @Operation(summary = "借閱書籍", description = "建立成功回 201 並帶 Location 標頭")
    @PostMapping("/{id}/borrow")
    public ResponseEntity<LoanVo> borrowBook(@PathVariable Long id,
                                             @Valid @RequestBody BorrowBookReq request) {
        LoanVo loan = loanServiceHandler.borrowBook(request.memberId(), id);
        return ResponseEntity.created(URI.create("/api/loans/" + loan.id())).body(loan);
    }
}
