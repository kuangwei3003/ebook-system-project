package com.kw.ebook.entity.vo;

import java.util.List;

/** 通用分頁回應。 */
public record PageVo<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext) {

    public static <T> PageVo<T> from(PageResult<T> result) {
        return new PageVo<>(result.items(), result.page(), result.size(),
                result.totalElements(), result.totalPages(), result.hasNext());
    }

    /** 列表頁僅需摘要資訊，由 Book 轉換。 */
    public record BookListItem(
            Long id,
            String title,
            String author,
            int availableCopies,
            int totalCopies) {

        public static BookListItem from(Book book) {
            return new BookListItem(book.getId(), book.getTitle(), book.getAuthor(),
                    book.getAvailableCopies(), book.getTotalCopies());
        }
    }
}
