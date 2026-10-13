package com.kw.ebook.controller;

import com.kw.ebook.entity.vo.LoanVo;
import com.kw.ebook.enums.LoanStatus;
import com.kw.ebook.service.LoanServiceHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 借閱單相關 API：歸還、單筆查詢、會員借閱清單。
 *
 * <p>「查看目前已借閱的書籍」以會員資源為視角：
 * GET /api/members/{id}/loans，預設只回傳借閱中（ACTIVE）的紀錄。</p>
 */
@Tag(name = "借閱", description = "歸還、單筆查詢與會員借閱清單")
@RestController
@RequestMapping("/api")
public class LoanController {

    @Resource
    private LoanServiceHandler loanServiceHandler;


    /** 歸還書籍：對借閱單資源的動作為「歸還」，以子資源 POST 表達。 */
    @Operation(summary = "歸還書籍")
    @PostMapping("/loans/{id}/return")
    public LoanVo returnLoan(@PathVariable Long id) {
        return loanServiceHandler.returnLoan(id);
    }

    /** 查詢單筆借閱。 */
    @Operation(summary = "查詢單筆借閱")
    @GetMapping("/loans/{id}")
    public LoanVo getLoan(@PathVariable Long id) {
        return loanServiceHandler.getLoan(id);
    }

    /** 查詢會員借閱紀錄，預設「目前借閱中」。 */
    @Operation(summary = "查詢會員借閱紀錄", description = "預設只回傳借閱中（ACTIVE）的紀錄")
    @GetMapping("/members/{memberId}/loans")
    public List<LoanVo> listMemberLoans(
            @PathVariable Long memberId,
            @RequestParam(defaultValue = "ACTIVE") LoanStatus status) {
        return loanServiceHandler.listLoans(memberId, status);
    }
}
