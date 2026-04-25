package dev.cinder.note.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class IpHasher {

    private final SaltProvider saltProvider;

    /**
     * Hashes an IP with the current rotating salt. Returns a hex string
     * suitable for use as a Redis key.
     */
    public String hash(String ip) {
        MessageDigest m;
        try {
            m = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable in this JVM", e);
        }

        m.update(ip.getBytes(StandardCharsets.UTF_8));
        m.update(saltProvider.currentSalt());
        return HexFormat.of().formatHex(m.digest());
    }
}