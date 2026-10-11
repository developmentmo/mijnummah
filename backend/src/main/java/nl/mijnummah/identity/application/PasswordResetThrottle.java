package nl.mijnummah.identity.application;

import nl.mijnummah.common.ratelimit.FixedWindowRateLimiter;

/**
 * Limits reset emails per (normalized) email address, so nobody can flood someone's inbox. Over the limit the
 * request is answered exactly as usual but no email is sent, so the answer never reveals whether an account exists.
 */
public class PasswordResetThrottle {

    private final FixedWindowRateLimiter requests;

    public PasswordResetThrottle(FixedWindowRateLimiter requests) {
        this.requests = requests;
    }

    /** Counts a request; false when this address already received its share of emails. */
    boolean tryAcquire(String normalizedEmail) {
        return requests.tryAcquire(normalizedEmail).allowed();
    }
}
