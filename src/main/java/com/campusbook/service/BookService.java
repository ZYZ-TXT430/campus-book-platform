package com.campusbook.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.campusbook.common.PageResult;
import com.campusbook.dto.BookPublishRequest;
import com.campusbook.dto.BookQuery;
import com.campusbook.entity.Book;

/**
 * 书籍业务：浏览、详情、发布、编辑、上下架、我的发布。
 *
 * <p>继承 IService&lt;Book&gt; 获得 getById / save / updateById / lambdaUpdate 等通用方法。</p>
 */
public interface BookService extends IService<Book> {

    /**
     * 分页查询在售书籍，按创建时间倒序。
     */
    PageResult<Book> page(BookQuery query);

    /**
     * 分页查询当前登录用户发布的书籍，包含下架、已售出等全部状态。
     */
    PageResult<Book> pageMine(BookQuery query, Integer status);

    /**
     * 书籍详情，浏览量 +1。
     */
    Book detail(Long id);

    /**
     * 发布书籍，卖家 ID 从当前登录上下文取，返回新书 ID。
     */
    Long publish(BookPublishRequest request);

    /**
     * 编辑自己发布的书：书名、作者、分类、价格、成色、描述等。
     * 已预订、已售出的书不允许编辑。
     */
    void updateBook(Long bookId, BookPublishRequest request);

    /**
     * 修改书籍状态：0-下架，1-重新上架。
     * 只有卖家本人可以操作。
     */
    void updateStatus(Long bookId, Integer status);
}
