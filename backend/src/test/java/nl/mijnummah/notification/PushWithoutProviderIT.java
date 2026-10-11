package nl.mijnummah.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import nl.mijnummah.AbstractIntegrationTest;
import nl.mijnummah.ApiTestClient.ApiResponse;

/** Default configuration (no push service): sending is refused with a clear code, nothing is recorded. */
class PushWithoutProviderIT extends AbstractIntegrationTest {

    @Test
    void sendingIsUnavailableAndTheAudienceSaysSo() {
        UUID mosqueId = mosque("noeroel-islam");
        Account admin = mosqueAdmin(mosqueId, "beheer");
        UUID id = UUID.fromString(api.post(adminPath(mosqueId, "announcements"), Map.of("title", "T", "body", "B"),
                admin.token()).field("id"));
        api.post(adminPath(mosqueId, "announcements/" + id + "/publish"), null, admin.token());

        ApiResponse refused = api.post(adminPath(mosqueId, "announcements/" + id + "/push"), null, admin.token());
        assertThat(refused.status()).isEqualTo(503);
        assertThat(refused.field("code")).isEqualTo("PUSH_NOT_CONFIGURED");
        assertThat(jdbc.queryForObject("select count(*) from push_notification", Integer.class)).isZero();

        ApiResponse audience = api.get(adminPath(mosqueId, "notifications/audience?contentType=ANNOUNCEMENT"),
                admin.token());
        assertThat(audience.body().get("configured").asBoolean()).isFalse();
    }
}
