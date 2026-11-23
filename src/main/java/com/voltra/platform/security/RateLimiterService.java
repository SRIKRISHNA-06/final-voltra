package com.voltra.platform.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {
    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> otpBuckets = new ConcurrentHashMap<>();

    // 5 login/register attempts per minute per IP
    public boolean allowAuthAttempt(String clientIp) {
        Bucket bucket = loginBuckets.computeIfAbsent(clientIp, k ->
                Bucket.builder()
                        .addLimit(Bandwidth.builder()
                                .capacity(5)
                                .refillIntervally(5, Duration.ofMinutes(1))
                                .build())
                        .build()
        );
        return bucket.tryConsume(1);
    }

    // 5 OTP operations per 5 minutes per user
    public boolean allowOtpAttempt(String userKey) {
        Bucket bucket = otpBuckets.computeIfAbsent(userKey, k ->
                Bucket.builder()
                        .addLimit(Bandwidth.builder()
                                .capacity(5)
                                .refillIntervally(5, Duration.ofMinutes(5))
                                .build())
                        .build()
        );
        return bucket.tryConsume(1);
    }
}
