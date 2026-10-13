package com.kw.ebook.entity.vo;

/** 書籍完整詳情回應。 */
public record BookDetailVo(
        Long id,
        String isbn,
        String title,
        String author,
        String description,
        int totalCopies,
        int availableCopies) {

    public static BookDetailVo from(Book book) {
        return new BookDetailVo(
                book.getId(), book.getIsbn(), book.getTitle(), book.getAuthor(),
                book.getDescription(), book.getTotalCopies(), book.getAvailableCopies());
    }
}
