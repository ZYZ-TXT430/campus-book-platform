package com.campusbook.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码加密器。
 *
 * <p>项目没有引入 spring-boot-starter-security，只用了 spring-security-crypto 的 BCrypt，
 * 因此不会触发 Spring Security 的默认登录页和过滤器链。</p>
 */
@Configuration
public class PasswordConfig {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
