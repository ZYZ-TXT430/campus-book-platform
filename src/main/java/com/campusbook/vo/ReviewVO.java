package com.campusbook.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 单条评价的展示对象。
 */
@Data
public class ReviewVO {

    private Long id;

    private Integer rating;

    private String content;

    /** 评价人昵称 */
    private String reviewerNickname;

    private LocalDateTime createdAt;
}
