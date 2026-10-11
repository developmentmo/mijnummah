package nl.mijnummah.notification.application;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import nl.mijnummah.notification.infrastructure.PushDeviceRepository;
import nl.mijnummah.notification.infrastructure.PushNotificationRepository;

/**
 * Delivers an accepted notification to every device, after the notification was committed and outside the request
 * thread. Records the counts and deletes devices whose token the push service no longer accepts.
 * <p>
 * Limitation (pilot scale): delivery state lives in memory until it is written; if the application stops halfway,
 * the notification stays {@code SENDING}. See D-034.
 */
@Component
class PushDispatcher {

    private static final Logger log = LoggerFactory.getLogger(PushDispatcher.class);

    private final PushGateway gateway;
    private final PushNotificationRepository notifications;
    private final PushDeviceRepository devices;
    private final Clock clock;

    PushDispatcher(PushGateway gateway, PushNotificationRepository notifications, PushDeviceRepository devices,
                   Clock clock) {
        this.gateway = gateway;
        this.notifications = notifications;
        this.devices = devices;
        this.clock = clock;
    }

    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(PushNotificationAccepted accepted) {
        int delivered = 0;
        int failed = 0;
        List<String> invalid = new ArrayList<>();
        boolean serviceFailed = false;
        for (String token : accepted.tokens()) {
            try {
                switch (gateway.send(accepted.message(), token)) {
                    case DELIVERED -> delivered++;
                    case INVALID_TOKEN -> {
                        invalid.add(token);
                        failed++;
                    }
                    case FAILED -> failed++;
                }
            } catch (PushServiceException ex) {
                log.error("Push service unusable, notification {} failed: {}", accepted.notificationId(),
                        ex.getMessage());
                serviceFailed = true;
                break;
            }
        }
        Instant now = Instant.now(clock);
        boolean failedCompletely = serviceFailed;
        int deliveredCount = delivered;
        int failedCount = failed;
        notifications.findById(accepted.notificationId()).ifPresent(notification -> {
            if (failedCompletely) {
                notification.fail(deliveredCount, now);
            } else {
                notification.complete(deliveredCount, failedCount, now);
            }
        });
        if (!invalid.isEmpty()) {
            devices.deleteByTokenIn(invalid);
        }
        log.info("Push notification {}: {} delivered, {} failed, {} invalid tokens removed",
                accepted.notificationId(), delivered, failed, invalid.size());
    }
}
