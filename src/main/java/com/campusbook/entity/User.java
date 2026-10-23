package com.campusbook.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户表 user 的实体。
 *
 * <p>MyBatis-Plus 约定：类名 User 对应表 user，字段 studentNo 对应 student_no，
 * 所以 @TableName 里只需要处理表名本身（user 在部分数据库里接近关键字，用反引号更稳）。</p>
 */
@Data
@TableName("`user`")
public class User {

    /** 数据库自增主键，插入后 MP 会回填到该字段 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 学号，登录账号，数据库有唯一索引 uk_student_no */
    private String studentNo;

    private String nickname;

    /**
     * BCrypt 密文。
     * @JsonIgnore 保证任何接口都不会把密码序列化给前端（包括 /api/users/me）。
     */
    @JsonIgnore
    private String password;

    private String realName;

    private String phone;

    private String email;

    private String campus;

    private String avatarUrl;

    /** 信用分，默认 100 */
    private Integer creditScore;

    /** 0-普通用户 1-管理员 */
    private Integer role;

    /** 1-正常 0-禁用 */
    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * 逻辑删除：0-未删 1-已删。
     * 加了 @TableLogic 后，MP 的 select/update 会自动追加 deleted = 0，
     * 调用 removeById 也不会真删，而是改成 deleted = 1。
     */
    @TableLogic
    private Integer deleted;
}
