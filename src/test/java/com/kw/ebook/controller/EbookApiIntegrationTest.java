package com.kw.ebook.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kw.ebook.entity.req.BorrowBookReq;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * API 層整合測試：以種子資料（dev profile）驗證端到端行為，
 * 涵蓋瀏覽、詳情、借閱、重複借閱、無庫存、歸還、重複歸還、目前借閱清單。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@DisplayName("E-Book 系統 API 整合測試")
class EbookApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String borrowBody(long memberId) throws Exception {
        return objectMapper.writeValueAsString(new BorrowBookReq(memberId));
    }

    @Test
    void 瀏覽書籍清單有分頁元數據() throws Exception {
        mockMvc.perform(get("/api/books").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.hasNext").value(true));
    }

    @Test
    void 關鍵字搜尋書籍() throws Exception {
        mockMvc.perform(get("/api/books").param("keyword", "evans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].title").value("Domain-Driven Design"));
    }

    @Test
    void 查詢書籍詳情() throws Exception {
        mockMvc.perform(get("/api/books/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.isbn").exists())
                .andExpect(jsonPath("$.description").exists());
    }

    @Test
    void 查詢不存在書籍回404與錯誤碼() throws Exception {
        mockMvc.perform(get("/api/books/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BOOK_NOT_FOUND"));
    }

    @Test
    void 借閱參數缺memberId回400() throws Exception {
        mockMvc.perform(post("/api/books/1/borrow")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void 借閱_歸還完整流程() throws Exception {
        // 借閱
        String response = mockMvc.perform(post("/api/books/1/borrow")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(borrowBody(1)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesRegex("/api/loans/\\d+")))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn().getResponse().getContentAsString();

        long loanId = objectMapper.readTree(response).get("id").asLong();

        // 借閱後庫存減一
        mockMvc.perform(get("/api/books/1"))
                .andExpect(jsonPath("$.availableCopies").value(2));

        // 同一會員重複借同一本書 → 409
        mockMvc.perform(post("/api/books/1/borrow")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(borrowBody(1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BOOK_ALREADY_BORROWED"));

        // 目前借閱中清單包含該筆
        mockMvc.perform(get("/api/members/1/loans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].bookTitle").value("Clean Architecture"));

        // 歸還
        mockMvc.perform(post("/api/loans/" + loanId + "/return"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED"))
                .andExpect(jsonPath("$.returnedAt").exists());

        // 歸還後庫存回復
        mockMvc.perform(get("/api/books/1"))
                .andExpect(jsonPath("$.availableCopies").value(3));

        // 重複歸還 → 409
        mockMvc.perform(post("/api/loans/" + loanId + "/return"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LOAN_ALREADY_RETURNED"));

        // 歸還後目前借閱清單為空
        mockMvc.perform(get("/api/members/1/loans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void 借閱已全數借出的書回409() throws Exception {
        // 種子資料中 id=4 的 availableCopies 為 0
        mockMvc.perform(post("/api/books/4/borrow")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(borrowBody(2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BOOK_UNAVAILABLE"));
    }

    @Test
    void 歸還不存在的借閱單回404() throws Exception {
        mockMvc.perform(post("/api/loans/9999/return"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LOAN_NOT_FOUND"));
    }
}
