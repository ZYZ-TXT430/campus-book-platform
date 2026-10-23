package com.campusbook.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 书籍表 book 的实体，一本书就是一件二手商品。
 */
@Data
@TableName("book")
public class Book {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 卖家 ID，发布时从 UserContext 取当前登录用户，不接收前端传值 */
    private Long sellerId;

    private String title;

    private String author;

    private String isbn;

    private String publisher;

    private LocalDate publishDate;

    private String category;

    private BigDecimal originalPrice;

    private BigDecimal price;

    /** 成色 1-10，10 表示全新 */
    private Integer conditionLevel;

    private Integer stock;

    private String coverUrl;

    private String description;

    /** 0-已下架 1-在售 2-已预订 3-已售出 */
    private Integer status;

    private Integer viewCount;

    /** 乐观锁版本号，下单扣库存时 +1 */
    private Integer version;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /** 逻辑删除：0-未删 1-已删，MP 自动追加 deleted = 0 */
    @TableLogic
    private Integer deleted;

    /**
     * 卖家昵称，不是 book 表的字段，查询后由 Service 批量填充，仅用于前端展示。
     */
    @TableField(exist = false)
    private String sellerNickname;
}
