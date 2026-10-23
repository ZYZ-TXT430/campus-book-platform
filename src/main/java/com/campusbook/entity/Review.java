package com.campusbook.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评价表 review 的实体。
 */
@Data
@TableName("review")
public class Review {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long bookId;

    /** 评价人（买家） */
    private Long reviewerId;

    /** 被评价人（卖家） */
    private Long revieweeId;

    /** 1-5 星 */
    private Integer rating;

    private String content;

    private LocalDateTime createdAt;
}
