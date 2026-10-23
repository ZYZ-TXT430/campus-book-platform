package com.campusbook.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.campusbook.common.BusinessException;
import com.campusbook.common.PageResult;
import com.campusbook.common.ResultCode;
import com.campusbook.context.UserContext;
import com.campusbook.dto.ReportCreateRequest;
import com.campusbook.entity.Book;
import com.campusbook.entity.Report;
import com.campusbook.entity.User;
import com.campusbook.mapper.BookMapper;
import com.campusbook.mapper.ReportMapper;
import com.campusbook.mapper.UserMapper;
import com.campusbook.service.ReportService;
import com.campusbook.vo.ReportVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl extends ServiceImpl<ReportMapper, Report> implements ReportService {

    private static final int STATUS_PENDING = 0;
    private static final int STATUS_OFFLINE = 1;
    private static final int STATUS_REJECTED = 2;

    private final BookMapper bookMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(ReportCreateRequest request) {
        Long reporterId = UserContext.requireUserId();

        Book book = bookMapper.selectById(request.getBookId());
        if (book == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "书籍不存在或已下架");
        }
        if (Objects.equals(book.getSellerId(), reporterId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不能举报自己发布的书籍");
        }

        // 同一用户对同一本书只保留一条待处理举报，避免刷屏
        boolean duplicated = lambdaQuery()
                .eq(Report::getReporterId, reporterId)
                .eq(Report::getBookId, request.getBookId())
                .eq(Report::getStatus, STATUS_PENDING)
                .exists();
        if (duplicated) {
            throw new BusinessException(ResultCode.CONFLICT, "你已经举报过这本书，管理员正在处理");
        }

        Report report = new Report();
        report.setReporterId(reporterId);
        report.setBookId(request.getBookId());
        report.setReason(request.getReason());
        report.setStatus(STATUS_PENDING);
        save(report);
    }

    @Override
    public PageResult<ReportVO> pageForAdmin(Integer status, Integer pageNum, Integer pageSize) {
        checkAdmin();

        int page = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        int size = (pageSize == null || pageSize < 1) ? 10 : Math.min(pageSize, 100);

        Page<Report> p = new Page<>(page, size);
        lambdaQuery()
                .eq(status != null, Report::getStatus, status)
                .orderByDesc(Report::getCreatedAt)
                .orderByDesc(Report::getId)
                .page(p);

        List<Report> records = p.getRecords();
        if (records.isEmpty()) {
            return PageResult.of(p.getTotal(), (int) p.getCurrent(), (int) p.getSize(), Collections.emptyList());
        }

        Set<Long> bookIds = records.stream().map(Report::getBookId).collect(Collectors.toSet());
        Set<Long> reporterIds = records.stream().map(Report::getReporterId).collect(Collectors.toSet());

        Map<Long, String> bookTitles = bookMapper.selectBatchIds(bookIds).stream()
                .collect(Collectors.toMap(Book::getId, Book::getTitle, (a, b) -> a));
        Map<Long, String> nicknames = userMapper.selectBatchIds(reporterIds).stream()
                .collect(Collectors.toMap(User::getId, User::getNickname, (a, b) -> a));

        List<ReportVO> vos = records.stream().map(r -> {
            ReportVO vo = new ReportVO();
            vo.setId(r.getId());
            vo.setBookId(r.getBookId());
            vo.setBookTitle(bookTitles.getOrDefault(r.getBookId(), "已删除书籍"));
            vo.setReporterId(r.getReporterId());
            vo.setReporterNickname(nicknames.getOrDefault(r.getReporterId(), "未知用户"));
            vo.setReason(r.getReason());
            vo.setStatus(r.getStatus());
            vo.setCreatedAt(r.getCreatedAt());
            vo.setHandledAt(r.getHandledAt());
            return vo;
        }).collect(Collectors.toList());

        return PageResult.of(p.getTotal(), (int) p.getCurrent(), (int) p.getSize(), vos);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handle(Long reportId, String action) {
        Long adminId = checkAdmin();

        Report report = getById(reportId);
        if (report == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "举报记录不存在");
        }
        if (!Integer.valueOf(STATUS_PENDING).equals(report.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "该举报已处理");
        }

        int target;
        if ("offline".equalsIgnoreCase(action)) {
            target = STATUS_OFFLINE;
            // 管理员下架书籍：只改状态，库存和订单历史保持原样
            LambdaUpdateWrapper<Book> bookUpdate = Wrappers.<Book>lambdaUpdate()
                    .eq(Book::getId, report.getBookId())
                    .set(Book::getStatus, 0);
            bookMapper.update(null, bookUpdate);
        } else if ("reject".equalsIgnoreCase(action)) {
            target = STATUS_REJECTED;
        } else {
            throw new BusinessException(ResultCode.BAD_REQUEST, "action 只能是 offline 或 reject");
        }

        boolean updated = lambdaUpdate()
                .eq(Report::getId, reportId)
                .eq(Report::getStatus, STATUS_PENDING)
                .set(Report::getStatus, target)
                .set(Report::getHandlerId, adminId)
                .set(Report::getHandledAt, LocalDateTime.now())
                .update();

        if (!updated) {
            throw new BusinessException(ResultCode.CONFLICT, "举报状态已变化，请刷新后重试");
        }
    }

    /**
     * 校验当前用户是不是管理员，返回用户 ID。
     */
    private Long checkAdmin() {
        Long userId = UserContext.requireUserId();
        User user = userMapper.selectById(userId);
        if (user == null || !Integer.valueOf(1).equals(user.getRole())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "需要管理员权限");
        }
        return userId;
    }
}
