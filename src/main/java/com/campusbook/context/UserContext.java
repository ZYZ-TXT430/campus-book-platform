package com.campusbook.context;

import com.campusbook.common.BusinessException;
import com.campusbook.common.ResultCode;

/**
 * 当前登录用户上下文，底层用 ThreadLocal 保存 userId。
 *
 * <p>Tomcat 处理每个请求用一个线程，拦截器在 preHandle 里 set，
 * Controller/Service 里随时 get，请求结束时 afterCompletion 里 remove。
 * 必须 remove：线程池会复用线程，不清会污染下一个请求，甚至造成越权。</p>
 *
 * <p>注意：异步任务（@Async、CompletableFuture）会切换线程，ThreadLocal 不会自动传递，
 * 需要手动把 userId 作为参数传过去，或者在任务提交时复制上下文。</p>
 */
public final class UserContext {

    private static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void setUserId(Long userId) {
        USER_ID_HOLDER.set(userId);
    }

    /**
     * 可能返回 null，调用方自己判断。
     */
    public static Long getUserId() {
        return USER_ID_HOLDER.get();
    }

    /**
     * 取当前用户，未登录直接抛 401。
     * Controller 里优先用这个，省掉一堆判空。
     */
    public static Long requireUserId() {
        Long userId = USER_ID_HOLDER.get();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return userId;
    }

    public static void clear() {
        USER_ID_HOLDER.remove();
    }
}
