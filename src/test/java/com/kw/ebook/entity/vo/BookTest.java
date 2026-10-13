package com.kw.ebook.entity.vo;

import com.kw.ebook.exception.DomainException;
import com.kw.ebook.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Book 庫存規則")
class BookTest {

    private Book newBook(int total, int available) {
        return new Book(1L, "978-x", "Title", "Author", "Desc", total, available);
    }

    @Test
    void 借閱成功時可借數減一() {
        Book book = newBook(3, 3);
        book.borrowOneCopy();
        assertEquals(2, book.getAvailableCopies());
    }

    @Test
    void 無可借副本時借閱被拒絕且庫存不受損() {
        Book book = newBook(1, 0);
        DomainException ex = assertThrows(DomainException.class, book::borrowOneCopy);
        assertEquals(ErrorCode.BOOK_UNAVAILABLE, ex.getErrorCode());
        assertEquals(0, book.getAvailableCopies());
        assertFalse(book.isAvailable());
    }

    @Test
    void 歸還時可借數加一() {
        Book book = newBook(2, 1);
        book.returnOneCopy();
        assertEquals(2, book.getAvailableCopies());
    }

    @Test
    void 歸還數量超過藏書總數屬系統異常() {
        Book book = newBook(1, 1);
        assertThrows(IllegalStateException.class, book::returnOneCopy);
    }

    @Test
    void 建立時庫存數值需合法() {
        assertThrows(IllegalArgumentException.class, () -> newBook(2, 3));
        assertThrows(IllegalArgumentException.class, () -> newBook(2, -1));
    }
}
