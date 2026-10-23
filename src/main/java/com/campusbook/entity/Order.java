package com.campusbook.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单表 orders 的实体。
 *
 * <p>order 是 MySQL 保留字，表名加了 s；MP 通过 @TableName 明确指定，避免生成非法 SQL。</p>
 */
@Data
@TableName("orders")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务订单号，对外展示，数据库有唯一索引 */
    private String orderNo;

    private Long bookId;

    /** 买家 ID，下单时从 UserContext 取 */
    private Long buyerId;

    /** 冗余的卖家 ID，便于"我卖出的"直接查询 */
    private Long sellerId;

    /** 下单时的书名快照 */
    private String bookTitle;

    /** 下单时的单价快照 */
    private BigDecimal unitPrice;

    private Integer quantity;

    private BigDecimal totalAmount;

    /** 0-待支付 1-已支付待交接 2-已完成 3-已取消 4-已退款 */
    private Integer status;

    /** 1-校内自提 2-快递 */
    private Integer deliveryType;

    private String deliveryAddr;

    private String remark;

    private LocalDateTime payTime;

    private LocalDateTime finishTime;

    private String cancelReason;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
