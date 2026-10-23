package com.campusbook.vo;

import lombok.Data;

import java.util.List;

/**
 * 一本书的评价汇总：平均分 + 数量 + 最近评价。
 */
@Data
public class ReviewSummaryVO {

    /** 平均分，保留一位小数；没有评价时为 0 */
    private double avgRating;

    private long count;

    private List<ReviewVO> reviews;
}
