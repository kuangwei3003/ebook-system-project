package com.kw.ebook.entity.vo;

import com.kw.ebook.exception.DomainException;
import com.kw.ebook.exception.ErrorCode;

/**
 * 一本書的藏書資訊與庫存。
 *
 * <p>庫存規則（借閱時必須尚有可借副本）由本實體自身守衛，
 * 外部不能直接修改可借數量，只能透過 borrowOneCopy / returnOneCopy。</p>
 */
public class Book {

    private final Long id;
    private final String isbn;
    private final String title;
    private final String author;
    private final String description;
    private final int totalCopies;

    /** 目前可借的副本數（Thread-safe 起見，僅能透過下方同步方法變更）。 */
    private int availableCopies;

    public Book(Long id, String isbn, String title, String author,
                String description, int totalCopies, int availableCopies) {
        if (totalCopies < 0 || availableCopies < 0 || availableCopies > totalCopies) {
            throw new IllegalArgumentException("無效的庫存數量");
        }
        this.id = id;
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.description = description;
        this.totalCopies = totalCopies;
        this.availableCopies = availableCopies;
    }

    /**
     * 借出一本副本。
     *
     * @throws DomainException ErrorCode.BOOK_UNAVAILABLE — 已無可借副本時
     */
    public synchronized void borrowOneCopy() {
        if (availableCopies <= 0) {
            throw new DomainException(ErrorCode.BOOK_UNAVAILABLE,
                    "書籍已無可借副本: " + title + " (id=" + id + ")");
        }
        availableCopies--;
    }

    /** 歸還一本副本，使可借數量加一。 */
    public synchronized void returnOneCopy() {
        if (availableCopies >= totalCopies) {
            throw new IllegalStateException("歸還數量超過藏書總數: " + id);
        }
        availableCopies++;
    }

    public boolean isAvailable() {
        return availableCopies > 0;
    }

    public Long getId() {
        return id;
    }

    public String getIsbn() {
        return isbn;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getDescription() {
        return description;
    }

    public int getTotalCopies() {
        return totalCopies;
    }

    public int getAvailableCopies() {
        return availableCopies;
    }
}
