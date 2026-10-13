package com.kw.ebook.enums;

/**
 * 借閱單的生命週期狀態。
 *
 * <p>狀態轉換：ACTIVE --(歸還)--> RETURNED，為不可逆的單向轉換。</p>
 */
public enum LoanStatus {
    ACTIVE,
    RETURNED
}
