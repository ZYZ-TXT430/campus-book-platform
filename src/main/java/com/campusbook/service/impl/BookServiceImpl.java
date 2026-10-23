package com.campusbook.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.campusbook.common.BusinessException;
import com.campusbook.common.PageResult;
import com.campusbook.common.ResultCode;
import com.campusbook.context.UserContext;
import com.campusbook.dto.BookPublishRequest;
import com.campusbook.dto.BookQuery;
import com.campusbook.entity.Book;
import com.campusbook.entity.User;
import com.campusbook.mapper.BookMapper;
import com.campusbook.mapper.UserMapper;
import com.campusbook.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookServiceImpl extends ServiceImpl<BookMapper, Book> implements BookService {

    private final UserMapper userMapper;

    @Override
    public PageResult<Book> page(BookQuery query) {
        query.normalize();

        Page<Book> page = new Page<>(query.getPageNum(), query.getPageSize());

        // @TableLogic 会自动追加 deleted = 0，这里只表达业务条件
        LambdaQueryWrapper<Book> wrapper = Wrappers.<Book>lambdaQuery()
                .eq(Book::getStatus, 1)
                .like(StringUtils.hasText(query.getKeyword()), Book::getTitle, query.getKeyword())
                .eq(StringUtils.hasText(query.getCategory()), Book::getCategory, query.getCategory())
                // 先按创建时间倒序，同一秒发布的再按 id 倒序，保证分页顺序稳定
                .orderByDesc(Book::getCreatedAt)
                .orderByDesc(Book::getId);

        page(page, wrapper);
        fillSellerNickname(page.getRecords());

        return PageResult.of(
                page.getTotal(),
                (int) page.getCurrent(),
                (int) page.getSize(),
                page.getRecords()
        );
    }

    @Override
    public PageResult<Book> pageMine(BookQuery query, Integer status) {
        query.normalize();

        // 卖家 ID 来自登录上下文，不接受前端传参
        Long sellerId = UserContext.requireUserId();

        Page<Book> page = new Page<>(query.getPageNum(), query.getPageSize());

        LambdaQueryWrapper<Book> wrapper = Wrappers.<Book>lambdaQuery()
                .eq(Book::getSellerId, sellerId)
                .eq(status != null, Book::getStatus, status)
                .like(StringUtils.hasText(query.getKeyword()), Book::getTitle, query.getKeyword())
                .orderByDesc(Book::getCreatedAt)
                .orderByDesc(Book::getId);

        page(page, wrapper);

        return PageResult.of(
                page.getTotal(),
                (int) page.getCurrent(),
                (int) page.getSize(),
                page.getRecords()
        );
    }

    @Override
    public Book detail(Long id) {
        Book book = getById(id);
        if (book == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "书籍不存在或已被删除");
        }
        baseMapper.increaseViewCount(id);
        fillSellerNickname(List.of(book));
        return book;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long publish(BookPublishRequest request) {
        // 卖家只能是当前登录用户，不信任前端传的 sellerId
        Long sellerId = UserContext.requireUserId();

        Book book = new Book();
        book.setSellerId(sellerId);
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setIsbn(request.getIsbn());
        book.setPublisher(request.getPublisher());
        book.setCategory(request.getCategory());
        book.setOriginalPrice(request.getOriginalPrice());
        book.setPrice(request.getPrice());
        book.setConditionLevel(request.getConditionLevel() == null ? 9 : request.getConditionLevel());
        // 二手书默认一本；同一个卖家有多个副本时再单独扩展
        book.setStock(1);
        book.setCoverUrl(request.getCoverUrl());
        book.setDescription(request.getDescription());
        book.setStatus(1);
        book.setViewCount(0);
        book.setVersion(0);
        book.setDeleted(0);

        save(book);
        return book.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBook(Long bookId, BookPublishRequest request) {
        Long userId = UserContext.requireUserId();

        Book book = getById(bookId);
        if (book == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "书籍不存在或已被删除");
        }
        if (!Objects.equals(book.getSellerId(), userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能编辑自己发布的书籍");
        }
        if (!Integer.valueOf(0).equals(book.getStatus()) && !Integer.valueOf(1).equals(book.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "已预订或已售出的书不能编辑");
        }

        boolean updated = lambdaUpdate()
                .eq(Book::getId, bookId)
                .eq(Book::getSellerId, userId)
                .in(Book::getStatus, 0, 1)
                .set(Book::getTitle, request.getTitle())
                .set(Book::getAuthor, request.getAuthor())
                .set(Book::getIsbn, request.getIsbn())
                .set(Book::getPublisher, request.getPublisher())
                .set(Book::getCategory, request.getCategory())
                .set(Book::getOriginalPrice, request.getOriginalPrice())
                .set(Book::getPrice, request.getPrice())
                .set(Book::getConditionLevel, request.getConditionLevel() == null ? 9 : request.getConditionLevel())
                .set(Book::getCoverUrl, request.getCoverUrl())
                .set(Book::getDescription, request.getDescription())
                .update();

        if (!updated) {
            throw new BusinessException(ResultCode.CONFLICT, "书籍状态已变化，请刷新后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long bookId, Integer status) {
        Long userId = UserContext.requireUserId();

        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "状态只能是 0（下架）或 1（上架）");
        }

        Book book = getById(bookId);
        if (book == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "书籍不存在或已被删除");
        }
        if (!Objects.equals(book.getSellerId(), userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能修改自己发布的书籍");
        }
        // 已预订(2)、已售出(3) 的书不能直接上下架，必须先走订单流程
        if (!Integer.valueOf(0).equals(book.getStatus()) && !Integer.valueOf(1).equals(book.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "当前状态不允许上下架");
        }
        if (status == 1 && (book.getStock() == null || book.getStock() <= 0)) {
            throw new BusinessException(ResultCode.CONFLICT, "库存为 0，无法重新上架");
        }

        // 条件更新：把归属和状态一起放进 WHERE，避免校验与更新之间被改掉
        boolean updated = lambdaUpdate()
                .eq(Book::getId, bookId)
                .eq(Book::getSellerId, userId)
                .in(Book::getStatus, 0, 1)
                .set(Book::getStatus, status)
                .update();

        if (!updated) {
            throw new BusinessException(ResultCode.CONFLICT, "书籍状态已变化，请刷新后重试");
        }
    }

    /**
     * 批量填充卖家昵称：一次 IN 查询，避免每本书都查一次 user 表。
     */
    private void fillSellerNickname(List<Book> books) {
        if (books == null || books.isEmpty()) {
            return;
        }
        Set<Long> sellerIds = books.stream()
                .map(Book::getSellerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (sellerIds.isEmpty()) {
            return;
        }
        Map<Long, String> nicknames = userMapper.selectBatchIds(sellerIds).stream()
                .collect(Collectors.toMap(User::getId, User::getNickname, (a, b) -> a));
        books.forEach(book -> book.setSellerNickname(nicknames.get(book.getSellerId())));
    }
}
