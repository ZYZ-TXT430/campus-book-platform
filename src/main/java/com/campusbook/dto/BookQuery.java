package com.campusbook.dto;

import lombok.Data;

/**
 * 书籍列表查询参数，由 Spring MVC 从 query string 自动绑定。
 */
@Data
public class BookQuery {

    /** 搜索关键字，只按书名模糊匹配 */
    private String keyword;

    /** 分类，精确匹配 */
    private String category;

    /** 页码，从 1 开始 */
    private Integer pageNum = 1;

    /** 每页条数，最大 100（与分页插件的 maxLimit 保持一致） */
    private Integer pageSize = 10;

    /**
     * 修正非法分页参数，避免负数页码或一次查出全表。
     */
    public void normalize() {
        if (pageNum == null || pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }
        if (pageSize > 100) {
            pageSize = 100;
        }
        if (keyword != null) {
            keyword = keyword.trim();
        }
        if (category != null) {
            category = category.trim();
        }
    }
}
