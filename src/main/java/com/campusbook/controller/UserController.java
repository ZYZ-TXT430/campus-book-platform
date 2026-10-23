package com.campusbook.controller;

import com.campusbook.common.Result;
import com.campusbook.entity.User;
import com.campusbook.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户信息接口（需要登录，走 LoginInterceptor）。
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 获取当前登录用户。
     *
     * <p>方法签名里没有 userId 参数：拦截器已经把 userId 放进 ThreadLocal，
     * Service 里通过 UserContext.requireUserId() 取。</p>
     *
     * <p>返回的 User 里 password 字段带 @JsonIgnore，不会被序列化。</p>
     */
    @GetMapping("/me")
    public Result<User> me() {
        return Result.success(userService.getCurrentUser());
    }
}
