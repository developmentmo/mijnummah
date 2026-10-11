package nl.mijnummah.identity.infrastructure;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Abuse protection, bound from {@code mijnummah.rate-limit.*}.
 *
 * @param enabled                 per-IP limits on/off (off in automated tests, which all come from one IP)
 * @param login                   {@code POST /auth/login} per IP
 * @param refresh                 {@code POST /auth/refresh} per IP
 * @param loginFailuresPerAccount failed logins per email address, regardless of IP (password guessing)
 */
@ConfigurationProperties(prefix = "mijnummah.rate-limit")
public record RateLimitProperties(Boolean enabled, Limit login, Limit refresh, Limit loginFailuresPerAccount) {

    public RateLimitProperties {
        enabled = enabled == null || enabled;
        login = orDefault(login, 10, Duration.ofMinutes(1));
        refresh = orDefault(refresh, 30, Duration.ofMinutes(1));
        loginFailuresPerAccount = orDefault(loginFailuresPerAccount, 10, Duration.ofMinutes(15));
    }

    /** At most {@code requests} per {@code per}. */
    public record Limit(int requests, Duration per) {

        public Limit {
            if (requests < 1 || per == null || per.isNegative() || per.isZero()) {
                throw new IllegalStateException("A rate limit needs requests >= 1 and a positive duration");
            }
        }
    }

    private static Limit orDefault(Limit limit, int requests, Duration per) {
        return limit != null ? limit : new Limit(requests, per);
    }
}
