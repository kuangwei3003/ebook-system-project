package com.kw.ebook.service;

import com.kw.ebook.entity.vo.Book;
import com.kw.ebook.entity.vo.PageResult;
import java.util.List;
import java.util.Optional;

/**
 * 書籍倉儲介面（Ports）：領域層只依賴介面，
 * 實際儲存由外層（infra）注入，日後可無縫替換為 RDB / Mongo 等實作。
 */
public interface BookService {

    /**
     * 以關鍵字（書名/作者/ISBN，不分大小寫）分頁查詢。
     *
     * @param keyword 可為 null 或空字串，表示不過濾
     */
    List<Book> search(String keyword, int offset, int limit);

    /** 符合關鍵字條件的書籍總數，提供分頁元數據。 */
    long countByKeyword(String keyword);

    Optional<Book> findById(Long id);

    Book save(Book book);
}
