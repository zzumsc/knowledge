package org.example.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
public class JwtTool {

    // JWT密钥（配置在application.yml，需至少32位字符）
    @Value("${jwt.secret}")
    private String secret;
    // JWT过期时间（默认2小时，可配置）
    @Value("${jwt.expire-hours:2}")
    private long expireHours;

    public String generateToken(Long userId) {
        // 1. 构建密钥（与解析时一致）
        SecretKey secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        // 2. 计算过期时间（当前时间 + 配置的小时数）
        Date expireDate = new Date(System.currentTimeMillis() + expireHours * 3600 * 1000);

        // 3. 生成JWT（设置签发时间、过期时间、负载（userId）、签名）
        return Jwts.builder()
                .setSubject(userId.toString()) // 主题：存储用户ID（字符串格式）
                .claim("user-id", userId) // 额外负载：显式存储userId（Long类型）
                .setIssuedAt(new Date()) // 签发时间
                .setExpiration(expireDate) // 过期时间
                .signWith(secretKey) // 签名（HMAC-SHA256算法）
                .compact();
    }
    public Long parseToken(String token) {
        // 1. 校验令牌非空
        if (token == null || token.trim().isEmpty()) {
            log.debug("令牌不能为空");
        }

        try {
            // 2. 构建加密密钥（HMAC-SHA256要求密钥至少256位）
            SecretKey secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

            // 3. 解析令牌，验证签名和有效性
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey) // 设置签名密钥
                    .build()
                    .parseClaimsJws(token) // 解析令牌（自动校验签名和过期时间）
                    .getBody();

            // 4. 提取用户ID（假设JWT中存储的键为"userId"）
            Long userId = claims.get("user-id", Long.class);
            if (userId == null) {
                log.debug("令牌中未包含用户ID");
            }

            // 5. 转换为Long类型返回
            return userId;

        } catch (ExpiredJwtException e) {
            log.debug("令牌已过期", e);
        } catch (MalformedJwtException e) {
            log.debug("令牌格式错误", e);
        } catch (UnsupportedJwtException e) {
            log.debug("不支持的令牌类型", e);
        } catch (IllegalArgumentException e) {
            log.debug("令牌参数非法", e);
        } catch (Exception e) {
            log.debug("令牌解析失败", e);
        }
        return null;
    }

    public Long getExpireHours() {
        return expireHours;
    }
}