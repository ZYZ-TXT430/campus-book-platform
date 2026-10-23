package com.campusbook.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 举报表 report 的实体。
 *
 * <p>status：0-待处理 1-已下架 2-已驳回。</p>
 */
@Data
@TableName("report")
public class Report {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 举报人 */
    private Long reporterId;

    /** 被举报的书 */
    private Long bookId;

    private String reason;

    private Integer status;

    /** 处理人（管理员） */
    private Long handlerId;

    private LocalDateTime handledAt;

    private LocalDateTime createdAt;
}
