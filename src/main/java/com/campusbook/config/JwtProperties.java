package com.campusbook.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置，对应 application.yml 中的 app.jwt.*。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /** 签名密钥，HS256 要求至少 32 字节 */
    private String secret;

    /** token 有效期（分钟） */
    private long expireMinutes = 120;

    /** 携带 token 的请求头名称 */
    private String header = "Authorization";

    /** token 前缀，注意包含末尾空格 */
    private String prefix = "Bearer ";
}
