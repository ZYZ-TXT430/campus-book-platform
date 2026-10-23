package com.campusbook.util;

import com.campusbook.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * JWT 生成与解析。
 *
 * <p>jjwt 0.13.x 的 API：构建用 Jwts.builder()，解析用 Jwts.parser().verifyWith(key)。</p>
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expireSeconds;

    public JwtUtil(JwtProperties properties) {
        byte[] keyBytes = properties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret 至少需要 32 字节，当前只有 " + keyBytes.length + " 字节");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.expireSeconds = properties.getExpireMinutes() * 60;
    }

    /**
     * 生成 token，subject 存用户 ID，附带学号方便前端展示。
     */
    public String generateToken(Long userId, String studentNo) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("studentNo", studentNo)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expireSeconds)))
                .signWith(key)
                .compact();
    }

    /**
     * 校验签名与过期时间并返回载荷，失败时抛 io.jsonwebtoken.JwtException。
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long getUserId(String token) {
        return Long.valueOf(parseToken(token).getSubject());
    }

    public long getExpireSeconds() {
        return expireSeconds;
    }
}
