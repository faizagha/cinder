package dev.cinder.note.security;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {

    private final IpHasher ipHasher;
    private final RateLimiter rateLimiter;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws IOException {

        String ip = extractClientIp(request);
        String hashedKey = "rate:" + ipHasher.hash(ip);

        boolean allowed = rateLimiter.tryAcquire(hashedKey);
        if (allowed) {
            return true; // continue to controller
        }

        // Rate limited — return 429 with a JSON body matching our error shape.
        log.info("rate limit exceeded for hashedKey={}", hashedKey);
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", "60");

        var body = Map.of(
                "timestamp", Instant.now().toString(),
                "status", 429,
                "error", "Too Many Requests",
                "message", "Rate limit exceeded. Please retry shortly.",
                "path", request.getRequestURI());
        response.getWriter().write(MAPPER.writeValueAsString(body));
        return false;
    }

    /**
     * Best-effort client IP extraction. Falls back to remote address if no proxy
     * header.
     * In production behind nginx/Cloudflare, X-Forwarded-For carries the real
     * client IP.
     */
    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // X-Forwarded-For can be "client, proxy1, proxy2" — take the first
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}