package com.campusbook.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.campusbook.common.BusinessException;
import com.campusbook.common.ResultCode;
import com.campusbook.context.UserContext;
import com.campusbook.dto.LoginRequest;
import com.campusbook.dto.RegisterRequest;
import com.campusbook.entity.User;
import com.campusbook.mapper.UserMapper;
import com.campusbook.service.UserService;
import com.campusbook.util.JwtUtil;
import com.campusbook.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户业务实现。
 *
 * <p>继承 ServiceImpl&lt;UserMapper, User&gt; 后，baseMapper 已经注入好了，
 * lambdaQuery() 也由父类提供，不需要自己写 SQL。</p>
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO register(RegisterRequest request) {
        // 数据库 uk_student_no 是最终防线，这里先查一次只是为了给出友好提示，
        // 高并发下两个请求可能同时通过这个判断，所以 insert 时仍要能处理唯一键冲突。
        boolean exists = lambdaQuery()
                .eq(User::getStudentNo, request.getStudentNo())
                .exists();
        if (exists) {
            throw new BusinessException(ResultCode.CONFLICT, "该学号已注册，请直接登录");
        }

        User user = new User();
        user.setStudentNo(request.getStudentNo());
        user.setNickname(request.getNickname());
        // 只存 BCrypt 密文；BCrypt 自带随机盐，同一密码每次加密结果都不同
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setCampus(request.getCampus());
        user.setCreditScore(100);
        user.setRole(0);
        user.setStatus(1);
        user.setDeleted(0);

        // ServiceImpl 提供的方法，等价于 baseMapper.insert(user)，
        // 且会把自增主键回填到 user.id
        save(user);
        return buildLoginVO(user);
    }

    @Override
    public LoginVO login(LoginRequest request) {
        User user = lambdaQuery()
                .eq(User::getStudentNo, request.getStudentNo())
                .one();

        // 用户不存在与密码错误返回同一提示，避免别人拿这个接口探测学号是否注册
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "学号或密码错误");
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已被禁用，请联系管理员");
        }
        return buildLoginVO(user);
    }

    @Override
    public User getCurrentUser() {
        // 未登录时 requireUserId() 会直接抛 401，不需要在 Controller 里判空
        Long userId = UserContext.requireUserId();
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    private LoginVO buildLoginVO(User user) {
        String token = jwtUtil.generateToken(user.getId(), user.getStudentNo());
        return LoginVO.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getExpireSeconds())
                .userId(user.getId())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .build();
    }
}
