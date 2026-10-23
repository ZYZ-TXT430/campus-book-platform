package com.campusbook.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.campusbook.dto.ReviewCreateRequest;
import com.campusbook.entity.Review;
import com.campusbook.vo.ReviewSummaryVO;

/**
 * 评价业务：买家评价订单、按书查看评价。
 */
public interface ReviewService extends IService<Review> {

    /**
     * 买家对已完成的订单评价卖家，并调整卖家信用分。
     */
    void create(ReviewCreateRequest request);

    /**
     * 查某本书的评价列表和平均分。
     */
    ReviewSummaryVO listByBook(Long bookId);
}
