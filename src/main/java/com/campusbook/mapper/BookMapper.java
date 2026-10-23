package com.campusbook.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campusbook.entity.Book;
import org.apache.ibatis.annotations.Param;

/**
 * 书籍表 Mapper。
 *
 * <p>单表增删改查由 BaseMapper 提供；这里只保留必须手写的原子 UPDATE：
 * 浏览量、扣库存、回滚库存。它们需要"在数据库里直接计算新值"的原子性。</p>
 */
public interface BookMapper extends BaseMapper<Book> {

    int increaseViewCount(@Param("id") Long id);

    /**
     * 原子扣减一本库存：在售才扣，扣到 0 自动标记为已售出（status=3）。
     * 返回 0 表示这本书已经不在售，调用方应抛"已被抢走"。
     */
    int decreaseStock(@Param("id") Long id);

    /**
     * 取消订单时回滚库存：数量 +1 并重新上架。
     * 必须在订单状态条件更新成功之后调用，保证只回滚一次。
     */
    int restoreStock(@Param("id") Long id);
}
