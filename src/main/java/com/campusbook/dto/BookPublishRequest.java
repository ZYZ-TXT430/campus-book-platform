package com.campusbook.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 发布二手书请求。
 */
@Data
public class BookPublishRequest {

    @NotBlank(message = "书名不能为空")
    @Size(max = 200, message = "书名不能超过 200 字")
    private String title;

    @Size(max = 100, message = "作者名过长")
    private String author;

    @Size(max = 20, message = "ISBN 过长")
    private String isbn;

    @Size(max = 100, message = "出版社名称过长")
    private String publisher;

    @Size(max = 50, message = "分类名称过长")
    private String category;

    @DecimalMin(value = "0.00", message = "原价不能为负数")
    private BigDecimal originalPrice;

    @NotNull(message = "售价不能为空")
    @DecimalMin(value = "0.00", message = "售价不能为负数")
    private BigDecimal price;

    @Min(value = 1, message = "成色最小为 1")
    @Max(value = 10, message = "成色最大为 10")
    private Integer conditionLevel = 9;

    @Size(max = 255, message = "封面地址过长")
    private String coverUrl;

    private String description;
}
