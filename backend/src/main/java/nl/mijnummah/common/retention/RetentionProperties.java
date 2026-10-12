package nl.mijnummah.common.retention;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * How long data is kept before the nightly clean-up removes it, bound from {@code mijnummah.retention.*}. The
 * periods are documented, with their reasons, in PRIVACY.md.
 *
 * @param enabled              clean-up on or off (off in automated tests, which call the clean-up directly)
 * @param cron                 when the nightly clean-up runs, in {@code Europe/Amsterdam}
 * @param expiredSessions      expired sessions (refresh tokens) are kept this long after they expired
 * @param expiredResetCodes    password reset codes are kept this long after they expired
 * @param eventRegistrations   registrations are kept this long after their event ended
 * @param inactivePushDevices  a push device the app has not reported for this long is removed
 * @param pushSendingTimeout   a push notification still "sending" after this long is marked as failed
 */
@ConfigurationProperties(prefix = "mijnummah.retention")
public record RetentionProperties(Boolean enabled, String cron, Duration expiredSessions, Duration expiredResetCodes,
                                  Duration eventRegistrations, Duration inactivePushDevices,
                                  Duration pushSendingTimeout) {

    public RetentionProperties {
        enabled = enabled == null || enabled;
        cron = cron == null || cron.isBlank() ? "0 30 3 * * *" : cron;
        expiredSessions = positiveOr(expiredSessions, Duration.ofDays(7));
        expiredResetCodes = positiveOr(expiredResetCodes, Duration.ofDays(1));
        eventRegistrations = positiveOr(eventRegistrations, Duration.ofDays(365));
        inactivePushDevices = positiveOr(inactivePushDevices, Duration.ofDays(270));
        pushSendingTimeout = positiveOr(pushSendingTimeout, Duration.ofMinutes(30));
    }

    private static Duration positiveOr(Duration value, Duration fallback) {
        if (value == null) {
            return fallback;
        }
        if (value.isNegative() || value.isZero()) {
            throw new IllegalStateException("Retention periods must be positive");
        }
        return value;
    }
}
