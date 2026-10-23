package com.campusbook.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campusbook.entity.Order;

/**
 * 订单表 Mapper。
 *
 * <p>订单都是简单的单表读写，BaseMapper 已经够用：
 * insert / selectById / selectList / selectPage 等，不需要写 XML。</p>
 *
 * <p>注意：扣库存的 UPDATE 不在这里，它操作的是 book 表，
 * 所以放在 {@link BookMapper#decreaseStock(Long)}。</p>
 */
public interface OrderMapper extends BaseMapper<Order> {
}
