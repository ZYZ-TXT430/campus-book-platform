package com.campusbook.common;

import lombok.Data;

import java.util.List;

/**
 * 分页返回结构，配合手写 LIMIT/OFFSET 查询使用。
 *
 * @param <T> 列表元素类型
 */
@Data
public class PageResult<T> {

    /** 总记录数 */
    private long total;

    /** 当前页码，从 1 开始 */
    private int page;

    /** 每页条数 */
    private int size;

    /** 当前页数据 */
    private List<T> records;

    public static <T> PageResult<T> of(long total, int page, int size, List<T> records) {
        PageResult<T> result = new PageResult<>();
        result.setTotal(total);
        result.setPage(page);
        result.setSize(size);
        result.setRecords(records);
        return result;
    }
}
