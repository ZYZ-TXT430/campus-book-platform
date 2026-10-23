package com.campusbook.controller;

import com.campusbook.common.PageResult;
import com.campusbook.common.Result;
import com.campusbook.dto.BookPublishRequest;
import com.campusbook.dto.BookQuery;
import com.campusbook.entity.Book;
import com.campusbook.service.BookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 书籍接口。
 *
 * <p>GET /api/books 与 /api/books/{id} 公开浏览；
 * /api/sell/** 需要登录，卖家 ID 由 Service 从 UserContext 取。</p>
 */
@RestController
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    /**
     * 分页查询在售书籍。
     *
     * @param query pageNum/pageSize/keyword/category，keyword 只匹配书名
     */
    @GetMapping("/api/books")
    public Result<PageResult<Book>> page(BookQuery query) {
        return Result.success(bookService.page(query));
    }

    @GetMapping("/api/books/{id}")
    public Result<Book> detail(@PathVariable Long id) {
        return Result.success(bookService.detail(id));
    }

    /**
     * 我发布的书籍，包含下架、已售出等全部状态。
     *
     * @param status 可选，按状态过滤：0-下架 1-在售 2-已预订 3-已售出
     */
    @GetMapping("/api/sell/books")
    public Result<PageResult<Book>> myBooks(BookQuery query,
                                            @RequestParam(required = false) Integer status) {
        return Result.success(bookService.pageMine(query, status));
    }

    @PostMapping("/api/sell/books")
    public Result<Long> publish(@Valid @RequestBody BookPublishRequest request) {
        return Result.success(bookService.publish(request));
    }

    /**
     * 编辑自己发布的书，只有卖家和 0/1 状态可以改。
     */
    @PutMapping("/api/sell/books/{id}")
    public Result<Void> updateBook(@PathVariable Long id,
                                   @Valid @RequestBody BookPublishRequest request) {
        bookService.updateBook(id, request);
        return Result.success();
    }

    /**
     * 上下架：status=0 下架，status=1 重新上架。
     */
    @PutMapping("/api/sell/books/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        bookService.updateStatus(id, status);
        return Result.success();
    }
}
