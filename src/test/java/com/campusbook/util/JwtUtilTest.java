package com.campusbook.util;

import com.campusbook.config.JwtProperties;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * JwtUtil 单元测试：不启动 Spring 上下文，因此不需要数据库。
 */
class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("unit-test-secret-key-at-least-32-bytes-long-0123456789");
        properties.setExpireMinutes(30);
        jwtUtil = new JwtUtil(properties);
    }

    @Test
    void shouldGenerateAndParseToken() {
        String token = jwtUtil.generateToken(42L, "20230001");

        Claims claims = jwtUtil.parseToken(token);

        assertEquals("42", claims.getSubject());
        assertEquals("20230001", claims.get("studentNo", String.class));
        assertEquals(42L, jwtUtil.getUserId(token));
        assertEquals(1800L, jwtUtil.getExpireSeconds());
    }

    @Test
    void shouldRejectTokenSignedByAnotherKey() {
        JwtProperties other = new JwtProperties();
        other.setSecret("another-secret-key-at-least-32-bytes-long-9876543210");
        JwtUtil otherUtil = new JwtUtil(other);
        String token = otherUtil.generateToken(1L, "20230002");

        assertThrows(Exception.class, () -> jwtUtil.parseToken(token));
    }

    @Test
    void shouldRejectTooShortSecret() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("too-short");

        assertThrows(IllegalStateException.class, () -> new JwtUtil(properties));
    }
}
