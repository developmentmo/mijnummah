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
 * Per-IP limits on the endpoints that can be abused without an account (login, refresh, registration and password
 * reset) and on the endpoints that check the current password of a signed-in user (changing the password and
 * deleting the account), so a stolen access token cannot be used to guess the password. Over the limit:
 * {@code 429 RATE_LIMITED} with {@code Retry-After}. Behind a reverse proxy set
 * {@code FORWARD_HEADERS_STRATEGY=framework} so the client IP is the real client, not the proxy.
 */
final class RateLimitFilter extends OncePerRequestFilter {

    /** Keyed by {@code "METHOD path"}. */
    private final Map<String, FixedWindowRateLimiter> limiters;
    private final SecurityErrorHandler errors;

    RateLimitFilter(RateLimitProperties properties, Clock clock, SecurityErrorHandler errors) {
        this.errors = errors;
        FixedWindowRateLimiter passwordCheck = limiter(properties.passwordCheck(), clock);
        this.limiters = Map.of(
                "POST /api/v1/auth/login", limiter(properties.login(), clock),
                "POST /api/v1/auth/refresh", limiter(properties.refresh(), clock),
                "POST /api/v1/auth/register", limiter(properties.register(), clock),
                "POST /api/v1/auth/password-reset", limiter(properties.passwordReset(), clock),
                "POST /api/v1/auth/password-reset/confirm", limiter(properties.passwordResetConfirm(), clock),
                "PUT /api/v1/me/password", passwordCheck,
                "POST /api/v1/me/deletion", passwordCheck);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        FixedWindowRateLimiter limiter = limiters.get(request.getMethod() + " " + request.getRequestURI());
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
