package nl.mijnummah.notification.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import nl.mijnummah.common.retention.RetentionConfiguration;
import nl.mijnummah.common.retention.RetentionProperties;
import nl.mijnummah.notification.PushStatus;
import nl.mijnummah.notification.domain.PushNotification;
import nl.mijnummah.notification.infrastructure.PushDeviceRepository;
import nl.mijnummah.notification.infrastructure.PushNotificationRepository;

/**
 * Push housekeeping: devices the app has not registered for a long time are removed (nightly), and a notification
 * that is still "sending" long after it started, because the backend stopped while sending, is marked as failed
 * (every few minutes, D-034). It is never sent again automatically: a duplicate is worse than a missed message.
 */
@Component
public class PushRetention {

    private static final Logger log = LoggerFactory.getLogger(PushRetention.class);

    private final PushDeviceRepository devices;
    private final PushNotificationRepository notifications;
    private final RetentionProperties retention;
    private final Clock clock;

    PushRetention(PushDeviceRepository devices, PushNotificationRepository notifications,
                  RetentionProperties retention, Clock clock) {
        this.devices = devices;
        this.notifications = notifications;
        this.retention = retention;
        this.clock = clock;
    }

    @Scheduled(cron = "${mijnummah.retention.cron:0 30 3 * * *}", zone = RetentionConfiguration.ZONE)
    public void purgeDevicesNow() {
        purgeDevices(Instant.now(clock));
    }

    @Transactional
    public void purgeDevices(Instant now) {
        int removed = devices.deleteLastSeenBefore(now.minus(retention.inactivePushDevices()));
        log.info("Retention: removed {} inactive push devices", removed);
    }

    @Scheduled(fixedDelayString = "PT5M", initialDelayString = "PT1M")
    public void failAbandonedNow() {
        failAbandoned(Instant.now(clock));
    }

    @Transactional
    public void failAbandoned(Instant now) {
        List<PushNotification> stuck = notifications.findByStatusAndCreatedAtBefore(
                PushStatus.SENDING, now.minus(retention.pushSendingTimeout()));
        for (PushNotification notification : stuck) {
            notification.fail(notification.view().deliveredCount(), now);
            log.warn("Push notification {} was still sending after {}; marked as failed", notification.getId(),
                    retention.pushSendingTimeout());
        }
    }
}
