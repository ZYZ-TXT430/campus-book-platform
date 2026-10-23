package com.campusbook.controller;

import com.campusbook.common.Result;
import com.campusbook.dto.ReviewCreateRequest;
import com.campusbook.service.ReviewService;
import com.campusbook.vo.ReviewSummaryVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 评价接口。
 */
@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * 买家评价已完成的订单。
     */
    @PostMapping("/api/reviews")
    public Result<Void> create(@Valid @RequestBody ReviewCreateRequest request) {
        reviewService.create(request);
        return Result.success();
    }

    /**
     * 查看某本书的评价，公开接口。
     */
    @GetMapping("/api/books/{id}/reviews")
    public Result<ReviewSummaryVO> listByBook(@PathVariable Long id) {
        return Result.success(reviewService.listByBook(id));
    }
}
