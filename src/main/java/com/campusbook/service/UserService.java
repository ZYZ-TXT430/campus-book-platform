package com.campusbook.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.campusbook.dto.LoginRequest;
import com.campusbook.dto.RegisterRequest;
import com.campusbook.entity.User;
import com.campusbook.vo.LoginVO;

/**
 * 用户业务：注册、登录、当前用户。
 *
 * <p>继承 IService&lt;User&gt; 后直接获得 getById、save、updateById、removeById、
 * list、page、lambdaQuery 等通用方法，不需要在接口里重复声明。</p>
 */
public interface UserService extends IService<User> {

    /**
     * 注册成功后直接签发 token，前端无需再调一次登录。
     */
    LoginVO register(RegisterRequest request);

    LoginVO login(LoginRequest request);

    /**
     * 从 ThreadLocal 取当前登录用户 ID 再查库，供 /api/users/me 使用。
     */
    User getCurrentUser();
}
