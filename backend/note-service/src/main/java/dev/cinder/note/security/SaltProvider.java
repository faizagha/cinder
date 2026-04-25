package dev.cinder.note.security;

import java.security.SecureRandom;
import java.time.Instant;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class SaltProvider {

    private static final SecureRandom RNG = new SecureRandom();

    private volatile byte[] currentSalt = generateSalt();
    private volatile Instant lastRotatedAt = Instant.now();

    @Scheduled(fixedRateString = "${cinder.rate-limit.salt-rotation-ms:3600000}")
    public void rotate() {
        currentSalt = generateSalt();
        lastRotatedAt = Instant.now();
        log.info("rate-limit salt rotated");
    }

    public void panicRotate() {
        currentSalt = generateSalt();
        lastRotatedAt = Instant.now();
        log.warn("PANIC: rate-limit salt rotated");
    }

    public byte[] currentSalt() {
        return currentSalt;
    }

    private byte[] generateSalt() {
        byte[] bytes = new byte[32];
        RNG.nextBytes(bytes);
        return bytes;
    }
}