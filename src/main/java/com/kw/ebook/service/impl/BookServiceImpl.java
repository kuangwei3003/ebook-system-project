package com.kw.ebook.service.impl;

import com.kw.ebook.entity.vo.Book;
import com.kw.ebook.service.BookService;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 記憶體實作：以 ConcurrentHashMap 儲存實體參照。
 *
 * <p>僅供展示用途，重啟即失資料；因領域介面已抽離，
 * 替換為 JPA/JDBC 實作時領域與服務層完全不用改動。</p>
 */
@Repository
public class BookServiceImpl implements BookService {

    private final Map<Long, Book> store = new ConcurrentHashMap<>();

    @Override
    public List<Book> search(String keyword, int offset, int limit) {
        return store.values().stream()
                .filter(book -> matches(book, keyword))
                .sorted((a, b) -> Long.compare(a.getId(), b.getId()))
                .skip(offset)
                .limit(limit)
                .toList();
    }

    @Override
    public long countByKeyword(String keyword) {
        return store.values().stream().filter(book -> matches(book, keyword)).count();
    }

    @Override
    public Optional<Book> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Book save(Book book) {
        store.put(book.getId(), book);
        return book;
    }

    /** 比對書名 / 作者 / ISBN，不分大小寫；關鍵字為空白時不過濾。 */
    private boolean matches(Book book, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String kw = keyword.trim().toLowerCase(Locale.ROOT);
        return book.getTitle().toLowerCase(Locale.ROOT).contains(kw)
                || book.getAuthor().toLowerCase(Locale.ROOT).contains(kw)
                || book.getIsbn().toLowerCase(Locale.ROOT).contains(kw);
    }
}
