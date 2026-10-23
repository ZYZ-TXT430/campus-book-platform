package com.campusbook.common;

import lombok.Getter;

/**
 * 统一业务状态码。
 *
 * <p>HTTP 状态码与业务码保持同一套数值，方便前端只判断一个字段。</p>
 */
@Getter
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数有误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "没有权限执行该操作"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "数据冲突"),
    ERROR(500, "服务器开小差了，请稍后再试");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
