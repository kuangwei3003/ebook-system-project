package com.kw.ebook.service;

import com.kw.ebook.entity.vo.Book;
import com.kw.ebook.entity.vo.PageResult;
import com.kw.ebook.exception.DomainException;
import com.kw.ebook.exception.ErrorCode;
import com.kw.ebook.service.BookService;
import java.util.List;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * 書籍應用服務：瀏覽與查詢用例的編排者。
 * 只負責「用例流程」，業務規則由領域物件自身守衛。
 */
@Service
public class BookServiceHandler {

    @Resource
    private BookService bookService;


    /** 分頁瀏覽書籍，可依關鍵字（書名/作者/ISBN）過濾。 */
    public PageResult<Book> searchBooks(String keyword, int page, int size) {
        if (page < 0 || size < 1) {
            throw new IllegalArgumentException("分頁參數無效: page=" + page + ", size=" + size);
        }
        List<Book> items = bookService.search(keyword, page * size, size);
        long total = bookService.countByKeyword(keyword);
        return new PageResult<>(items, page, size, total);
    }

    /** 查詢書籍詳情，不存在時拋出領域例外（由 API 層轉為 404）。 */
    public Book getBookDetail(Long bookId) {
        return bookService.findById(bookId)
                .orElseThrow(() -> new DomainException(
                        ErrorCode.BOOK_NOT_FOUND, "書籍不存在: id=" + bookId));
    }
}
