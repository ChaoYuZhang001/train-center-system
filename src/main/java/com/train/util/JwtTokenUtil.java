package com.train.util;

import com.train.security.JwtUserDetails;
import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@Slf4j
public class JwtTokenUtil{
    /**
     * JWT 密钥（配置在application.yml中）
     */
    @Value("${jwt.secret}")
    private String secret;

    /**
     * JWT 过期时间（秒，配置在application.yml中，默认3600秒=1小时）
     */
    @Value("${jwt.expiration:3600}")
    private Long expiration;

    @Resource
    private RedisUtil redisUtil;

    // 统一Key前缀
    private static final String TOKEN_BLACKLIST_PREFIX = "token:invalid:";
    private static final String USER_INFO_PREFIX = "user:info:";

    /**
     * 生成JWT令牌
     */
    public String generateToken(JwtUserDetails userDetails) {
        log.info("生成Token时的JWT密钥：{}，密钥长度：{}", secret, secret == null ? 0 : secret.length());
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userDetails.getUserId());
        claims.put("account", userDetails.getAccount());
        claims.put("userName", userDetails.getUsername());
        claims.put("orgId", userDetails.getOrgId());
        claims.put("isSysAdmin", userDetails.getIsSysAdmin());
        claims.put("authorities", userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));
        if (userDetails.getPermissions() != null) {
            claims.put("permissions", userDetails.getPermissions());
        } else {
            claims.put("permissions", userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList()));
        }

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date())
//                .setExpiration(new Date(System.currentTimeMillis() + expiration * 1000))
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact();

    }

    /**
     * 解析JWT令牌（针对性捕获异常，确保过期Token返回null）
     */
    public Claims getClaimsFromToken(String token) {
        log.info("验证Token时的JWT密钥：{}，密钥长度：{}", secret, secret == null ? 0 : secret.length());
        try {
            return Jwts.parser()
                    .setSigningKey(secret)
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            log.warn("JWT Token已过期: {}", token.substring(Math.max(0, token.length() - 10)));
            return null;
        } catch (MalformedJwtException | SignatureException | IllegalArgumentException e) {
            log.error("JWT Token无效: {}", token.substring(Math.max(0, token.length() - 10)), e);
            return null;
        } catch (Exception e) {
            log.error("JWT Token解析失败", e);
            return null;
        }
    }

    /**
     * 验证JWT令牌是否有效（用户名匹配 + 未过期 + 非黑名单）
     */
    public boolean validateToken(String token, JwtUserDetails userDetails) {
        if (userDetails == null) {
            return false;
        }
        Claims claims = getClaimsFromToken(token);
        if (claims == null || isTokenInBlacklist(token)) {
            return false;
        }
        String subject = claims.getSubject();
        return subject != null && subject.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    /**
     * 校验Token是否有效（独立校验：未过期 + 非黑名单 + 能解析）
     */
    public boolean isTokenValid(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        // 1. 优先校验黑名单
        if (isTokenInBlacklist(token)) {
            return false;
        }
        // 2. 校验JWT是否过期且能解析
        Claims claims = getClaimsFromToken(token);
        if (claims == null) {
            return false;
        }
        return !isTokenExpired(token);
    }

    /**
     * 判断Token是否过期
     */
    private boolean isTokenExpired(String token) {
        Date expirationDate = getExpirationFromToken(token);
        return expirationDate != null && expirationDate.before(new Date());
    }

    /**
     * 获取Token剩余时间（秒）
     */
    public long getTokenRemainingTime(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims == null) {
            return 0;
        }
        Date expirationDate = claims.getExpiration();
        if (expirationDate == null) {
            return 0;
        }
        long remainingMs = expirationDate.getTime() - System.currentTimeMillis();
        return Math.max(0, remainingMs / 1000); // 转换为秒，最小为0
    }

    /**
     * 构建用户信息Redis键名
     */
    public String buildUserInfoKey(String token) {
        return USER_INFO_PREFIX + token;
    }

    /**
     * 更新Redis用户信息缓存过期时间（无感续期核心方法）
     */
    public boolean refreshUserInfoCache(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        String userInfoKey = buildUserInfoKey(token);
        if (!redisUtil.hasKey(userInfoKey)) {
            return false;
        }
        // 更新过期时间为原始JWT过期时间
        redisUtil.expire(userInfoKey, expiration, TimeUnit.SECONDS);
        log.info("Token用户信息缓存已续期: {}", token.substring(Math.max(0, token.length() - 10)));
        return true;
    }

    public Date getExpirationFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims == null) {
            return null;
        }
        return claims.getExpiration();
    }

    public Date getExpirationDateFromToken(String token) {
        return getExpirationFromToken(token);
    }

    public Long getExpiration() {
        return this.expiration;
    }

    public boolean isTokenInBlacklist(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        String blacklistKey = TOKEN_BLACKLIST_PREFIX + token;
        return redisUtil.hasKey(blacklistKey);
    }

    public void addTokenToBlacklist(String token) {
        if (token == null || token.trim().isEmpty()) {
            return;
        }
        long remainingTime = getTokenRemainingTime(token);
        if (remainingTime <= 0) {
            return;
        }
        String blacklistKey = TOKEN_BLACKLIST_PREFIX + token;
        redisUtil.set(blacklistKey, "invalid", remainingTime, TimeUnit.SECONDS);
        log.info("Token已加入黑名单: {}", token.substring(Math.max(0, token.length() - 10)));
    }

    public long getExpirationTimeFromToken(String token) {
        Date expiration = getExpirationFromToken(token);
        if (expiration == null) {
            return 0;
        }
        long timeLeft = expiration.getTime() - System.currentTimeMillis();
        return Math.max(timeLeft, 0);
    }


    public void setRedis(String key, Object value, long timeout, TimeUnit timeUnit) {
         redisUtil.set(key, value, timeout, timeUnit);
    }
    public boolean hasKey(String userInfoKey) {
        return redisUtil.hasKey(userInfoKey);
    }

    public Object getRedis(String userInfoKey) {
        return redisUtil.get(userInfoKey);
    }

}