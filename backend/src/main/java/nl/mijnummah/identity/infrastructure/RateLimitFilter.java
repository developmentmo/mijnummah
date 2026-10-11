package nl.mijnummah.identity.infrastructure;

import java.io.IOException;
import java.time.Clock;
import java.util.Map;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.web.filter.OncePerRequestFilter;

import nl.mijnummah.common.ratelimit.FixedWindowRateLimiter;
import nl.mijnummah.common.ratelimit.FixedWindowRateLimiter.Decision;

/**
 * Per-IP limits on the endpoints that can be abused without an account: login and refresh. Over the limit:
 * {@code 429 RATE_LIMITED} with {@code Retry-After}. Behind a reverse proxy set
 * {@code FORWARD_HEADERS_STRATEGY=framework} so the client IP is the real client, not the proxy.
 */
final class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, FixedWindowRateLimiter> limitersByPath;
    private final SecurityErrorHandler errors;

    RateLimitFilter(RateLimitProperties properties, Clock clock, SecurityErrorHandler errors) {
        this.errors = errors;
        this.limitersByPath = Map.of(
                "/api/v1/auth/login", limiter(properties.login(), clock),
                "/api/v1/auth/refresh", limiter(properties.refresh(), clock));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        FixedWindowRateLimiter limiter = "POST".equals(request.getMethod())
                ? limitersByPath.get(request.getRequestURI())
                : null;
        if (limiter != null) {
            Decision decision = limiter.tryAcquire(request.getRemoteAddr());
            if (!decision.allowed()) {
                errors.tooManyRequests(request, response, decision.retryAfter());
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private static FixedWindowRateLimiter limiter(RateLimitProperties.Limit limit, Clock clock) {
        return new FixedWindowRateLimiter(limit.requests(), limit.per(), clock);
    }
}
