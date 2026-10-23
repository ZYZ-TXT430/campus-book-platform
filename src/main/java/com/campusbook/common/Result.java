package com.campusbook.common;

import lombok.Data;

/**
 * 统一返回结果类。
 *
 * <p>所有 Controller 接口都返回这个结构，前端只判断 code 即可：</p>
 *
 * <pre>
 * {
 *   "code": 200,
 *   "message": "操作成功",
 *   "data": { ... }
 * }
 * </pre>
 *
 * <p>成功用 {@link #success()} / {@link #success(Object)}，
 * 失败用 {@link #error()} / {@link #error(String)} / {@link #error(Integer, String)}。</p>
 *
 * @param <T> 业务数据类型
 */
@Data
public class Result<T> {

    /**
     * 业务状态码。
     * 200-成功，400-参数错误，401-未登录，403-无权限，
     * 404-资源不存在，409-数据冲突，500-服务器异常
     */
    private Integer code;

    /** 提示信息，可直接展示给用户 */
    private String message;

    /** 业务数据，失败时为 null */
    private T data;

    /**
     * 保留无参构造，方便 Jackson 反序列化。
     * 例如测试里用 RestTemplate 接收响应时，需要能 new 出对象再 set 字段。
     */
    public Result() {
    }

    private Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // ==================== 成功 ====================

    /**
     * 成功但不返回数据，例如删除、退出登录。
     */
    public static <T> Result<T> success() {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    /**
     * 成功并返回业务数据，最常用的写法：return Result.success(user);
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    /**
     * 成功并自定义提示语，例如"注册成功，请先完善资料"。
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), message, data);
    }

    // ==================== 失败 ====================

    /**
     * 通用失败，code=500，message="服务器开小差了，请稍后再试"。
     */
    public static <T> Result<T> error() {
        return error(ResultCode.ERROR);
    }

    /**
     * 指定提示语的失败，code 默认 500。
     * 业务校验失败一般用 error(400, "...") 或 error(ResultCode.BAD_REQUEST)。
     */
    public static <T> Result<T> error(String message) {
        return error(ResultCode.ERROR.getCode(), message);
    }

    /**
     * 最底层的方法：显式指定 code 和 message。
     */
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    /**
     * 用枚举表达失败，避免到处写魔法数字。
     */
    public static <T> Result<T> error(ResultCode resultCode) {
        return error(resultCode.getCode(), resultCode.getMessage());
    }

    /**
     * 用枚举 + 自定义提示语，例如 Result.error(ResultCode.CONFLICT, "该学号已注册")。
     */
    public static <T> Result<T> error(ResultCode resultCode, String message) {
        return error(resultCode.getCode(), message);
    }
}
