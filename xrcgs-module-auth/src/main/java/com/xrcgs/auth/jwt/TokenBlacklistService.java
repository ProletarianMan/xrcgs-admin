package com.xrcgs.auth.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;

/**
 * Redis 黑名单服务
 */
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final StringRedisTemplate redis;
    private static final String KEY_PREFIX = "jwt:blacklist:";

    public void blacklist(String jti, long secondsToLive) {
        if (jti == null) return;
        String key = KEY_PREFIX + jti;
        redis.opsForValue().set(key, "1", Duration.ofSeconds(Math.max(secondsToLive, 1)));
    }

    public boolean isBlacklisted(String jti) {
        if (jti == null) return false;
        Boolean exists = redis.hasKey(KEY_PREFIX + jti);
        return exists != null && exists;
    }

    /** 便捷：直接用 token 计算剩余 TTL 并拉黑 */
    public void blacklistToken(String token, JwtUtil jwtUtil) {
        String jti = jwtUtil.getJti(token);
        Date exp = jwtUtil.getExpiration(token);
        long ttl = exp == null ? 0 : Math.max(0, (exp.getTime() - System.currentTimeMillis()) / 1000);
        blacklist(jti, ttl);
    }
}
