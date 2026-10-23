package com.campusbook.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 登录成功后返回给前端的信息。
 */
@Data
@Builder
public class LoginVO {

    private String token;

    private String tokenType;

    /** 有效期（秒） */
    private long expiresIn;

    private Long userId;

    private String nickname;

    private String avatarUrl;

    /** 角色：0-普通用户 1-管理员，前端据此决定是否显示管理入口 */
    private Integer role;
}
