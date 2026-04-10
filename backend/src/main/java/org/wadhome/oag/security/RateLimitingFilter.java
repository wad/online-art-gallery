package org.wadhome.oag.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final String VIEWS_PATH = "/api/v1/public/views";

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final int viewsPerMinute;

    public RateLimitingFilter(@Value("${oag.rate-limit.views-per-minute:60}") int viewsPerMinute) {
        this.viewsPerMinute = viewsPerMinute;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        if ("POST".equals(request.getMethod()) && VIEWS_PATH.equals(request.getRequestURI())) {
            String ip = getClientIp(request);
            Bucket bucket = buckets.computeIfAbsent(ip, k -> newBucket());
            if (!bucket.tryConsume(1)) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.getWriter().write("{\"status\":429,\"title\":\"Too Many Requests\"}");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private Bucket newBucket() {
        return Bucket.builder()
            .addLimit(Bandwidth.builder()
                .capacity(viewsPerMinute)
                .refillGreedy(viewsPerMinute, Duration.ofMinutes(1))
                .build())
            .build();
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].strip();
        }
        return request.getRemoteAddr();
    }
}
