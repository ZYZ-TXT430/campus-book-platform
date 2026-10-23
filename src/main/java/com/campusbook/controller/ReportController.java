package com.campusbook.controller;

import com.campusbook.common.PageResult;
import com.campusbook.common.Result;
import com.campusbook.dto.ReportCreateRequest;
import com.campusbook.service.ReportService;
import com.campusbook.vo.ReportVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 举报接口：普通用户提交举报，管理员查看和处理。
 *
 * <p>/api/** 已经被登录拦截器覆盖，这里不再重复鉴权；
 * 管理员身份在 Service 里校验。</p>
 */
@RestController
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /**
     * 举报书籍。
     */
    @PostMapping("/api/reports")
    public Result<Void> create(@Valid @RequestBody ReportCreateRequest request) {
        reportService.create(request);
        return Result.success();
    }

    /**
     * 管理端举报列表。
     */
    @GetMapping("/api/admin/reports")
    public Result<PageResult<ReportVO>> page(@RequestParam(required = false) Integer status,
                                             @RequestParam(defaultValue = "1") Integer pageNum,
                                             @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(reportService.pageForAdmin(status, pageNum, pageSize));
    }

    /**
     * 处理举报：action=offline 下架书籍，action=reject 驳回。
     */
    @PostMapping("/api/admin/reports/{id}/handle")
    public Result<Void> handle(@PathVariable Long id, @RequestParam String action) {
        reportService.handle(id, action);
        return Result.success();
    }
}
