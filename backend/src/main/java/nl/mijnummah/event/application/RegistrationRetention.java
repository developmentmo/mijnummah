package nl.mijnummah.event.application;

import java.time.Clock;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import nl.mijnummah.common.retention.RetentionConfiguration;
import nl.mijnummah.common.retention.RetentionProperties;
import nl.mijnummah.event.infrastructure.EventRegistrationRepository;

/**
 * Nightly removal of registrations some time after their event ended (default one year, PRIVACY.md). The event itself
 * stays; only who registered for it is forgotten.
 */
@Component
public class RegistrationRetention {

    private static final Logger log = LoggerFactory.getLogger(RegistrationRetention.class);

    private final EventRegistrationRepository registrations;
    private final RetentionProperties retention;
    private final Clock clock;

    RegistrationRetention(EventRegistrationRepository registrations, RetentionProperties retention, Clock clock) {
        this.registrations = registrations;
        this.retention = retention;
        this.clock = clock;
    }

    @Scheduled(cron = "${mijnummah.retention.cron:0 30 3 * * *}", zone = RetentionConfiguration.ZONE)
    public void purgeNow() {
        purge(Instant.now(clock));
    }

    @Transactional
    public void purge(Instant now) {
        int removed = registrations.deleteForEventsEndedBefore(now.minus(retention.eventRegistrations()));
        log.info("Retention: removed {} registrations for past events", removed);
    }
}
