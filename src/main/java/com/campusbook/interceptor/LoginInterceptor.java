package com.campusbook.interceptor;

import com.campusbook.common.BusinessException;
import com.campusbook.common.ResultCode;
import com.campusbook.config.JwtProperties;
import com.campusbook.context.UserContext;
import com.campusbook.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录拦截器。
 *
 * <p>职责只有两个：校验 Authorization 头里的 JWT，把 userId 放进 UserContext；
 * 请求结束时清理 ThreadLocal。</p>
 *
 * <p>不在这里查数据库：JWT 已经签名，解析出来的 userId 是可信的；
 * 每次请求都查一次用户表只会增加无谓的数据库压力。
 * 如果确实需要实时拦截被封禁的用户，可以在 preHandle 里加一次缓存查询。</p>
 */
@Component
@RequiredArgsConstructor
public class LoginInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 跨域预检请求不带 token，直接放行
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        String authorization = request.getHeader(jwtProperties.getHeader());
        String prefix = jwtProperties.getPrefix();
        if (authorization == null || !authorization.startsWith(prefix)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        String token = authorization.substring(prefix.length()).trim();
        try {
            Long userId = jwtUtil.getUserId(token);
            UserContext.setUserId(userId);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // 签名不对、格式错误、已过期都会走到这里
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 无论请求成功还是抛异常都要清理，否则线程池复用时会把上一个用户带进来
        UserContext.clear();
    }
}
