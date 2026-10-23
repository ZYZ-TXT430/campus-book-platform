package com.campusbook.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.campusbook.common.BusinessException;
import com.campusbook.common.ResultCode;
import com.campusbook.context.UserContext;
import com.campusbook.dto.ReviewCreateRequest;
import com.campusbook.entity.Order;
import com.campusbook.entity.Review;
import com.campusbook.entity.User;
import com.campusbook.mapper.OrderMapper;
import com.campusbook.mapper.ReviewMapper;
import com.campusbook.mapper.UserMapper;
import com.campusbook.service.ReviewService;
import com.campusbook.vo.ReviewSummaryVO;
import com.campusbook.vo.ReviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl extends ServiceImpl<ReviewMapper, Review> implements ReviewService {

    private static final int STATUS_FINISHED = 2;

    private final OrderMapper orderMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(ReviewCreateRequest request) {
        Long reviewerId = UserContext.requireUserId();

        Order order = orderMapper.selectById(request.getOrderId());
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }
        if (!Objects.equals(order.getBuyerId(), reviewerId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有买家可以评价这笔订单");
        }
        if (!Integer.valueOf(STATUS_FINISHED).equals(order.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "订单完成后才能评价");
        }

        boolean reviewed = lambdaQuery()
                .eq(Review::getOrderId, order.getId())
                .eq(Review::getReviewerId, reviewerId)
                .exists();
        if (reviewed) {
            throw new BusinessException(ResultCode.CONFLICT, "这笔订单已经评价过了");
        }

        Review review = new Review();
        review.setOrderId(order.getId());
        review.setBookId(order.getBookId());
        review.setReviewerId(reviewerId);
        review.setRevieweeId(order.getSellerId());
        review.setRating(request.getRating());
        review.setContent(request.getContent());
        save(review);

        // 信用分变化：5 星 +2，4 星 +1，3 星不变，2 星 -2，1 星 -5，限制在 0-150
        int delta = switch (request.getRating()) {
            case 5 -> 2;
            case 4 -> 1;
            case 3 -> 0;
            case 2 -> -2;
            default -> -5;
        };
        if (delta != 0) {
            userMapper.update(null, Wrappers.<User>lambdaUpdate()
                    .eq(User::getId, order.getSellerId())
                    .setSql("credit_score = LEAST(150, GREATEST(0, credit_score + " + delta + "))"));
        }
    }

    @Override
    public ReviewSummaryVO listByBook(Long bookId) {
        List<Review> reviews = lambdaQuery()
                .eq(Review::getBookId, bookId)
                .orderByDesc(Review::getCreatedAt)
                .orderByDesc(Review::getId)
                .last("LIMIT 20")
                .list();

        ReviewSummaryVO summary = new ReviewSummaryVO();
        summary.setCount(reviews.size());
        if (reviews.isEmpty()) {
            summary.setAvgRating(0);
            summary.setReviews(Collections.emptyList());
            return summary;
        }

        double avg = reviews.stream().mapToInt(Review::getRating).average().orElse(0);
        summary.setAvgRating(Math.round(avg * 10) / 10.0);

        Set<Long> reviewerIds = reviews.stream().map(Review::getReviewerId).collect(Collectors.toSet());
        Map<Long, String> nicknames = userMapper.selectBatchIds(reviewerIds).stream()
                .collect(Collectors.toMap(User::getId, User::getNickname, (a, b) -> a));

        List<ReviewVO> vos = reviews.stream().map(r -> {
            ReviewVO vo = new ReviewVO();
            vo.setId(r.getId());
            vo.setRating(r.getRating());
            vo.setContent(r.getContent());
            vo.setReviewerNickname(nicknames.getOrDefault(r.getReviewerId(), "匿名同学"));
            vo.setCreatedAt(r.getCreatedAt());
            return vo;
        }).collect(Collectors.toList());

        summary.setReviews(vos);
        return summary;
    }
}
