package dev.cinder.note.security;

import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RateLimiter {

    private final StringRedisTemplate redis;

    @Value("${cinder.rate-limit.capacity:60}")
    private long capacity;

    @Value("${cinder.rate-limit.refill-rate:1.0}")
    private double refillRate;

    @Value("${cinder.rate-limit.bucket-ttl-ms:3600000}")
    private long bucketTtlMs;

    private DefaultRedisScript<Long> script;

    @PostConstruct
    public void loadScript() {
        script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("scripts/rate_limit.lua"));
        script.setResultType(Long.class);
    }

    public boolean tryAcquire(String key) {
        Long result = redis.execute(
            script,
            List.of(key),
            String.valueOf(capacity),
            String.valueOf(refillRate),
            String.valueOf(Instant.now().toEpochMilli()),
            String.valueOf(bucketTtlMs)
        );
        return result != null && result == 1L;
    }
}