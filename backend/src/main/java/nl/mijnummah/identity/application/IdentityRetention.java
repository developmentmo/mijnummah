package nl.mijnummah.identity.application;

import java.time.Clock;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import nl.mijnummah.common.retention.RetentionConfiguration;
import nl.mijnummah.common.retention.RetentionProperties;
import nl.mijnummah.identity.infrastructure.PasswordResetCodeRepository;
import nl.mijnummah.identity.infrastructure.RefreshTokenRepository;

/** Nightly removal of expired sessions and password reset codes (PRIVACY.md, retention). */
@Component
public class IdentityRetention {

    private static final Logger log = LoggerFactory.getLogger(IdentityRetention.class);

    private final RefreshTokenRepository refreshTokens;
    private final PasswordResetCodeRepository resetCodes;
    private final RetentionProperties retention;
    private final Clock clock;

    IdentityRetention(RefreshTokenRepository refreshTokens, PasswordResetCodeRepository resetCodes,
                      RetentionProperties retention, Clock clock) {
        this.refreshTokens = refreshTokens;
        this.resetCodes = resetCodes;
        this.retention = retention;
        this.clock = clock;
    }

    @Scheduled(cron = "${mijnummah.retention.cron:0 30 3 * * *}", zone = RetentionConfiguration.ZONE)
    public void purgeNow() {
        purge(Instant.now(clock));
    }

    @Transactional
    public void purge(Instant now) {
        int sessions = refreshTokens.deleteExpiredBefore(now.minus(retention.expiredSessions()));
        int codes = resetCodes.deleteExpiredBefore(now.minus(retention.expiredResetCodes()));
        log.info("Retention: removed {} expired sessions and {} expired password reset codes", sessions, codes);
    }
}
