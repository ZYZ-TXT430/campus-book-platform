package com.campusbook.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端举报列表的展示对象，比实体多了书名和举报人昵称。
 */
@Data
public class ReportVO {

    private Long id;

    private Long bookId;

    private String bookTitle;

    private Long reporterId;

    private String reporterNickname;

    private String reason;

    /** 0-待处理 1-已下架 2-已驳回 */
    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime handledAt;
}
