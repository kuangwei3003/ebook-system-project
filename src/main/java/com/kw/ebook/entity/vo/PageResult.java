package com.kw.ebook.entity.vo;

import java.util.List;

/**
 * 輕量分頁值對象：避免領域層綁架特定框架（如 Spring Data 的 Page）。
 */
public record PageResult<T>(List<T> items, int page, int size, long totalElements) {

    public int totalPages() {
        return size <= 0 ? 0 : (int) Math.ceil((double) totalElements / size);
    }

    public boolean hasNext() {
        return (long) (page + 1) * size < totalElements;
    }
}
