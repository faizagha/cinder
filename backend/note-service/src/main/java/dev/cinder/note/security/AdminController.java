package dev.cinder.note.security;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final SaltProvider saltProvider;
    private final StringRedisTemplate redis;

    @Value("${cinder.admin.token:}")
    private String adminToken;

    @PostMapping("/panic-wipe")
    public ResponseEntity<?> panicWipe(@RequestHeader("X-Admin-Token") String token) {
        // Reject if token is unset (no admin operations on misconfigured deployments)
        // OR if the provided token doesn't match.
        if (adminToken.isBlank() || !constantTimeEquals(token, adminToken)) {
            log.warn("panic-wipe attempted with invalid token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        saltProvider.panicRotate();

        Set<String> keys = redis.keys("rate:*");
        long wiped = 0;
        if (keys != null && !keys.isEmpty()) {
            Long deleted = redis.delete(keys);
            wiped = deleted == null ? 0 : deleted;
        }

        log.warn("PANIC WIPE EXECUTED — salt rotated, {} rate-limit keys deleted", wiped);
        return ResponseEntity.ok(Map.of(
                "rotated", true,
                "rateKeysWiped", wiped,
                "at", Instant.now().toString()));
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null)
            return false;
        return MessageDigest.isEqual(a.getBytes(), b.getBytes());
    }
}