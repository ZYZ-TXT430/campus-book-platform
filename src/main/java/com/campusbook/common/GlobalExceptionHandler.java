package com.campusbook.common;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器。
 *
 * <p>@RestControllerAdvice 会扫描所有 Controller 抛出的异常，交给下面的方法处理，
 * 这样业务代码里只负责 throw，不用每个接口都写 try-catch。</p>
 *
 * <p>这里统一返回 HTTP 200 + Result，由前端根据 code 判断结果。
 * 如果希望 HTTP 状态码也保持语义（401 就返回 HTTP 401），
 * 把方法返回值改成 ResponseEntity&lt;Result&lt;Void&gt;&gt;，
 * 再用 ResponseEntity.status(code).body(...) 包装即可。</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 自定义业务异常：属于可预期的失败（学号已注册、库存不足、无权查看订单等），
     * 用 warn 级别记录，不打印堆栈，避免日志被业务异常刷屏。
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常：code={}, message={}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * @RequestBody + @Valid 校验失败，例如书名空、价格负数。
     * 把每个字段的错误拼成一句"title 书名不能为空；price 售价不能为空"。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("；"));
        log.warn("参数校验失败：{}", message);
        return Result.error(ResultCode.BAD_REQUEST.getCode(), message);
    }

    /**
     * 表单/查询参数绑定失败（非 @RequestBody）。
     * 注意 MethodArgumentNotValidException 是 BindException 的子类，
     * Spring 会优先匹配更具体的那个处理器。
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("；"));
        log.warn("参数绑定失败：{}", message);
        return Result.error(ResultCode.BAD_REQUEST.getCode(), message);
    }

    /**
     * 方法参数上的约束校验失败（类上标了 @Validated，参数上写 @Min/@NotBlank 时触发）。
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + " " + v.getMessage())
                .collect(Collectors.joining("；"));
        log.warn("参数校验失败：{}", message);
        return Result.error(ResultCode.BAD_REQUEST.getCode(), message);
    }

    /**
     * 请求体不是合法 JSON，或字段类型对不上（例如 price 传了 "abc"）。
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败：{}", e.getMessage());
        return Result.error(ResultCode.BAD_REQUEST.getCode(), "请求体格式错误，请检查 JSON 是否合法");
    }

    /**
     * 访问了不存在的接口，避免被下面的 Exception 兜底成 500。
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public Result<Void> handleNoResourceFound(NoResourceFoundException e) {
        return Result.error(ResultCode.NOT_FOUND.getCode(), "接口不存在：" + e.getResourcePath());
    }

    /**
     * 兜底：所有没被上面处理器接住的异常。
     * 这里必须打印完整堆栈，否则线上问题无从排查；但返回给前端的只能是笼统提示，不暴露内部细节。
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常，未捕获：", e);
        return Result.error(ResultCode.ERROR);
    }

    /**
     * 统一格式：字段名 + 校验提示。
     */
    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + " " + fieldError.getDefaultMessage();
    }
}
