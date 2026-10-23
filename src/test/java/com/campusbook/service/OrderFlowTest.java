package com.campusbook.service;

import com.campusbook.common.BusinessException;
import com.campusbook.context.UserContext;
import com.campusbook.dto.BookPublishRequest;
import com.campusbook.dto.OrderCreateRequest;
import com.campusbook.dto.RegisterRequest;
import com.campusbook.entity.Book;
import com.campusbook.mapper.BookMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 订单核心流程集成测试。
 *
 * <p>@Transactional 让每个测试方法结束后回滚，不会污染开发库；
 * 需要本机 MySQL 正在运行。</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class OrderFlowTest {

    @Autowired
    private UserService userService;

    @Autowired
    private BookService bookService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private BookMapper bookMapper;

    @AfterEach
    void clearContext() {
        UserContext.clear();
    }

    @Test
    void buyThenCancelShouldRestoreStock() {
        Long sellerId = registerUser("卖家");
        UserContext.setUserId(sellerId);
        Long bookId = bookService.publish(publishRequest("测试教材"));

        Long buyerId = registerUser("买家");
        UserContext.setUserId(buyerId);
        Long orderId = orderService.create(orderRequest(bookId));

        Book afterBuy = bookMapper.selectById(bookId);
        assertEquals(0, afterBuy.getStock(), "下单后库存应为 0");
        assertEquals(3, afterBuy.getStatus(), "最后一本卖出后状态应为已售出");

        orderService.cancel(orderId);

        Book afterCancel = bookMapper.selectById(bookId);
        assertEquals(1, afterCancel.getStock(), "取消后库存应回滚为 1");
        assertEquals(1, afterCancel.getStatus(), "取消后书籍应重新上架");
    }

    @Test
    void cannotBuySoldOutBook() {
        Long sellerId = registerUser("卖家");
        UserContext.setUserId(sellerId);
        Long bookId = bookService.publish(publishRequest("只剩一本"));

        Long buyerId = registerUser("买家");
        UserContext.setUserId(buyerId);
        orderService.create(orderRequest(bookId));

        assertThrows(BusinessException.class,
                () -> orderService.create(orderRequest(bookId)),
                "重复购买同一本书应失败");
    }

    @Test
    void cannotBuyOwnBook() {
        Long sellerId = registerUser("自买自卖");
        UserContext.setUserId(sellerId);
        Long bookId = bookService.publish(publishRequest("自己的书"));

        assertThrows(BusinessException.class,
                () -> orderService.create(orderRequest(bookId)),
                "不能购买自己发布的书籍");
    }

    private Long registerUser(String nickname) {
        RegisterRequest request = new RegisterRequest();
        request.setStudentNo("99" + String.valueOf(System.nanoTime()).substring(5, 13));
        request.setNickname(nickname);
        request.setPassword("123456");
        return userService.register(request).getUserId();
    }

    private BookPublishRequest publishRequest(String title) {
        BookPublishRequest request = new BookPublishRequest();
        request.setTitle(title);
        request.setAuthor("测试作者");
        request.setCategory("教材");
        request.setPrice(new BigDecimal("12.50"));
        request.setOriginalPrice(new BigDecimal("39.00"));
        request.setConditionLevel(8);
        return request;
    }

    private OrderCreateRequest orderRequest(Long bookId) {
        OrderCreateRequest request = new OrderCreateRequest();
        request.setBookId(bookId);
        request.setDeliveryType(1);
        return request;
    }
}
