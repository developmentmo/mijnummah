package nl.mijnummah.retention;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import nl.mijnummah.AbstractIntegrationTest;
import nl.mijnummah.event.application.RegistrationRetention;
import nl.mijnummah.identity.application.IdentityRetention;
import nl.mijnummah.notification.application.PushRetention;

/** The clean-up jobs remove exactly what PRIVACY.md says, and nothing that is still in use. */
class RetentionIT extends AbstractIntegrationTest {

    @Autowired
    private IdentityRetention identityRetention;

    @Autowired
    private RegistrationRetention registrationRetention;

    @Autowired
    private PushRetention pushRetention;

    @Test
    void expiredSessionsAndResetCodesAreRemovedAfterTheirGracePeriod() {
        Account account = user("sessie");
        jdbc.update("""
                insert into password_reset_code (id, user_id, code_hash, created_at, expires_at, attempts, version)
                values (?, ?, 'hash', now(), now() + interval '30 minutes', 0, 0)""", UUID.randomUUID(), account.id());

        // a day later the 30-day session is still valid, and the code is within its one-day grace period
        identityRetention.purge(Instant.now().plus(Duration.ofDays(1)));
        assertThat(count("refresh_token")).isEqualTo(2);
        assertThat(count("password_reset_code")).isEqualTo(1);
        assertThat(api.get("/api/v1/auth/me", account.token()).status()).isEqualTo(200);

        identityRetention.purge(Instant.now().plus(Duration.ofDays(2)));
        assertThat(count("password_reset_code")).isZero();
        assertThat(count("refresh_token")).isEqualTo(2);

        // 30 days validity + 7 days grace
        identityRetention.purge(Instant.now().plus(Duration.ofDays(38)));
        assertThat(count("refresh_token")).isZero();
        assertThat(count("app_user")).as("accounts themselves stay").isEqualTo(2);
    }

    @Test
    void registrationsAreForgottenAYearAfterTheEventButTheEventStays() {
        UUID mosqueId = mosque("noeroel-islam");
        Account admin = mosqueAdmin(mosqueId, "beheer");
        Map<String, Object> body = new HashMap<>(eventBody("Lezing", inDays(5)));
        body.put("registrationEnabled", true);
        UUID eventId = publishedEvent(mosqueId, body, admin.token());
        Account visitor = user("bezoeker");
        assertThat(api.post("/api/v1/events/" + eventId + "/registrations", null, visitor.token()).status())
                .isEqualTo(201);

        registrationRetention.purge(Instant.now().plus(Duration.ofDays(5 + 364)));
        assertThat(count("event_registration")).isEqualTo(1);

        registrationRetention.purge(Instant.now().plus(Duration.ofDays(5 + 366)));
        assertThat(count("event_registration")).isZero();
        assertThat(count("event")).isEqualTo(1);
    }

    @Test
    void devicesTheAppStoppedReportingAreRemoved() {
        Account visitor = user("toestel");
        api.put("/api/v1/me/devices", Map.of("token", "token-1", "platform", "ANDROID"), visitor.token());

        pushRetention.purgeDevices(Instant.now().plus(Duration.ofDays(269)));
        assertThat(count("push_device")).isEqualTo(1);

        pushRetention.purgeDevices(Instant.now().plus(Duration.ofDays(271)));
        assertThat(count("push_device")).isZero();
    }

    @Test
    void aNotificationStillSendingAfterHalfAnHourIsMarkedFailedOnce() {
        UUID mosqueId = mosque("noeroel-islam");
        Account admin = mosqueAdmin(mosqueId, "beheer");
        UUID notificationId = UUID.randomUUID();
        jdbc.update("""
                insert into push_notification (id, mosque_id, content_type, content_id, title, body, status,
                    recipient_count, delivered_count, failed_count, created_by, created_at, version)
                values (?, ?, 'ANNOUNCEMENT', ?, 'Titel', 'Tekst', 'SENDING', 3, 0, 0, ?, now(), 0)""",
                notificationId, mosqueId, UUID.randomUUID(), admin.id());

        pushRetention.failAbandoned(Instant.now().plus(Duration.ofMinutes(29)));
        assertThat(status(notificationId)).isEqualTo("SENDING");

        pushRetention.failAbandoned(Instant.now().plus(Duration.ofMinutes(31)));
        assertThat(status(notificationId)).isEqualTo("FAILED");
        assertThat(jdbc.queryForObject("select failed_count from push_notification where id = ?", Integer.class,
                notificationId)).isEqualTo(3);
        assertThat(count("push_notification")).as("the record stays, so it is never sent twice").isEqualTo(1);
    }

    private int count(String table) {
        return jdbc.queryForObject("select count(*) from " + table, Integer.class);
    }

    private String status(UUID notificationId) {
        return jdbc.queryForObject("select status from push_notification where id = ?", String.class, notificationId);
    }
}
