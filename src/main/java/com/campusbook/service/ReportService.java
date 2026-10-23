package com.campusbook.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.campusbook.common.PageResult;
import com.campusbook.dto.ReportCreateRequest;
import com.campusbook.entity.Report;
import com.campusbook.vo.ReportVO;

/**
 * 举报业务：用户举报书籍、管理员查看与处理。
 */
public interface ReportService extends IService<Report> {

    /**
     * 普通用户举报一本书。
     */
    void create(ReportCreateRequest request);

    /**
     * 管理端举报列表，按时间倒序。
     *
     * @param status 0-待处理 1-已下架 2-已驳回，null 表示全部
     */
    PageResult<ReportVO> pageForAdmin(Integer status, Integer pageNum, Integer pageSize);

    /**
     * 管理员处理举报。
     *
     * @param action offline-下架书籍并标记已处理；reject-驳回举报
     */
    void handle(Long reportId, String action);
}
