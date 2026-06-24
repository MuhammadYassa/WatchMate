package com.project.watchmate.common.ratelimit;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.project.watchmate.common.error.ApiError;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

/**
 * Per-IP rate limiter for auth-sensitive endpoints backed by Bucket4j.
 *
 * <p>Buckets are stored in a {@link ConcurrentHashMap} in JVM heap memory.
 * This is per-instance only and not distributed, in a multi-instance
 * deployment each JVM has independent limits, so a client can exhaust up to
 * capacity * instanceCount tokens by routing across instances.
 * For true distributed rate limiting replace the map with a Bucket4j/Redis
 *
 * To prevent unbounded memory growth, a scheduled job evicts bucket entries
 * that have not been accessed within {watchmate.rate-limit.bucket-ttl-seconds}
 * (default 3600 s). The job runs every
 * {watchmate.rate-limit.bucket-cleanup-interval-seconds} (default 300 s).
 */
@Component
@RequiredArgsConstructor
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AuthRateLimitFilter.class);

    private static final Map<String, String> ENDPOINT_KEYS = Map.of(
        "/api/v1/auth/login",         "login",
        "/api/v1/auth/register",      "register",
        "/api/v1/auth/verify/resend", "resend",
        "/api/v1/auth/refresh",       "refresh"
    );

    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;

    // Keyed by "endpointKey:clientIp". Per-instance only — see class javadoc.
    private final ConcurrentHashMap<String, BucketEntry> buckets = new ConcurrentHashMap<>();

    /** Holds a Bucket4j bucket together with the epoch-millis of the last access. */
    record BucketEntry(Bucket bucket, AtomicLong lastAccessEpochMillis) {}

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!properties.isEnabled()) {
            chain.doFilter(request, response);
            return;
        }

        String endpointKey = ENDPOINT_KEYS.get(request.getRequestURI());
        if (endpointKey == null) {
            chain.doFilter(request, response);
            return;
        }

        String clientIp = extractClientIp(request);
        String bucketKey = endpointKey + ":" + clientIp;

        BucketEntry entry = buckets.computeIfAbsent(
            bucketKey,
            k -> new BucketEntry(buildBucket(endpointKey), new AtomicLong(System.currentTimeMillis()))
        );
        entry.lastAccessEpochMillis().set(System.currentTimeMillis());

        if (entry.bucket().tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded path={} ip={}", request.getRequestURI(), clientIp);
            sendRateLimitResponse(response);
        }
    }

    private Bucket buildBucket(String endpointKey) {
        int capacity;
        long refillSeconds;
        switch (endpointKey) {
            case "login"    -> { capacity = properties.getLoginCapacity();    refillSeconds = properties.getLoginRefillSeconds(); }
            case "register" -> { capacity = properties.getRegisterCapacity(); refillSeconds = properties.getRegisterRefillSeconds(); }
            case "resend"   -> { capacity = properties.getResendCapacity();   refillSeconds = properties.getResendRefillSeconds(); }
            case "refresh"  -> { capacity = properties.getRefreshCapacity();  refillSeconds = properties.getRefreshRefillSeconds(); }
            default -> throw new IllegalArgumentException("Unknown endpoint key: " + endpointKey);
        }
        return Bucket.builder()
            .addLimit(Bandwidth.builder()
                .capacity(capacity)
                .refillGreedy(capacity, Duration.ofSeconds(refillSeconds))
                .build())
            .build();
    }

    private String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String xri = request.getHeader("X-Real-IP");
        if (xri != null && !xri.isBlank()) {
            return xri;
        }
        return request.getRemoteAddr();
    }

    private void sendRateLimitResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", "60");
        objectMapper.writeValue(
            response.getWriter(),
            new ApiError("Too many requests. Please slow down and try again later.", "TOO_MANY_REQUESTS", null)
        );
    }

    /**
     * Evicts bucket entries that have not been accessed within the configured TTL.
     * Runs on a fixed-delay schedule; also callable directly in tests via the
     * package-private {@link #removeStaleEntries(long)} overload.
     *
     * <p>When {@code watchmate.rate-limit.enabled=false} the bucket map is never
     * populated, so this is always a no-op.
     */
    @Scheduled(fixedDelayString = "#{${watchmate.rate-limit.bucket-cleanup-interval-seconds:300} * 1000}")
    void cleanupStaleBuckets() {
        removeStaleEntries(System.currentTimeMillis());
    }

    /**
     * Removes entries whose last-access time is older than the configured TTL
     * relative to {@code nowEpochMillis}. Package-private so unit tests can inject
     * a synthetic "now" without sleeping real time.
     *
     * <p>A bucket is only evicted based on inactivity age — an empty (exhausted)
     * bucket is never removed solely because it has no remaining tokens.
     */
    void removeStaleEntries(long nowEpochMillis) {
        long ttlMs = properties.getBucketTtlSeconds() * 1000L;
        int before = buckets.size();
        buckets.entrySet().removeIf(e -> nowEpochMillis - e.getValue().lastAccessEpochMillis().get() > ttlMs);
        int removed = before - buckets.size();
        if (removed > 0) {
            log.debug("Rate-limit bucket cleanup: removed={} remaining={}", removed, buckets.size());
        }
    }
}
