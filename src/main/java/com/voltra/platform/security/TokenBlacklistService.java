package com.voltra.platform.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenBlacklistService {
    private static final String PREFIX = "voltra:jwt:blacklist:";

    private final StringRedisTemplate redisTemplate;
    private final boolean redisEnabled;
    private final Map<String, Long> localFallback = new ConcurrentHashMap<>();

    public TokenBlacklistService(@Autowired(required = false) StringRedisTemplate redisTemplate,
                                 @Value("${voltra.redis.enabled:false}") boolean redisEnabled) {
        this.redisTemplate = redisTemplate;
        this.redisEnabled = redisEnabled;
    }

    public void revokeToken(String token, long expiryEpochMs) {
        long ttlMs = expiryEpochMs - System.currentTimeMillis();
        if (ttlMs <= 0) return;

        if (redisEnabled && redisTemplate != null) {
            redisTemplate.opsForValue().set(PREFIX + token, "revoked", Duration.ofMillis(ttlMs));
            return;
        }
        localFallback.put(token, expiryEpochMs);
        long now = System.currentTimeMillis();
        localFallback.entrySet().removeIf(e -> e.getValue() < now);
    }

    public boolean isRevoked(String token) {
        if (redisEnabled && redisTemplate != null) {
            return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX + token));
        }
        Long exp = localFallback.get(token);
        if (exp == null) return false;
        if (exp < System.currentTimeMillis()) {
            localFallback.remove(token);
            return false;
        }
        return true;
    }
}
