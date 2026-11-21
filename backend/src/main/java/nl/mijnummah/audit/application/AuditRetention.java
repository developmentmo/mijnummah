package nl.mijnummah.audit.application;

import java.time.Clock;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import nl.mijnummah.audit.infrastructure.AuditLogRepository;
import nl.mijnummah.common.retention.RetentionConfiguration;
import nl.mijnummah.common.retention.RetentionProperties;

/** Nightly removal of old audit log entries (PRIVACY.md, retention). */
@Component
public class AuditRetention {

    private static final Logger log = LoggerFactory.getLogger(AuditRetention.class);

    private final AuditLogRepository entries;
    private final RetentionProperties retention;
    private final Clock clock;

    AuditRetention(AuditLogRepository entries, RetentionProperties retention, Clock clock) {
        this.entries = entries;
        this.retention = retention;
        this.clock = clock;
    }

    @Scheduled(cron = "${mijnummah.retention.cron:0 30 3 * * *}", zone = RetentionConfiguration.ZONE)
    public void purgeNow() {
        purge(Instant.now(clock));
    }

    @Transactional
    public void purge(Instant now) {
        int removed = entries.deleteOccurredBefore(now.minus(retention.auditLog()));
        log.info("Retention: removed {} audit log entries", removed);
    }
}
