package nl.mijnummah.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import tools.jackson.databind.JsonNode;

import nl.mijnummah.AbstractIntegrationTest;
import nl.mijnummah.ApiTestClient.ApiResponse;
import nl.mijnummah.common.mail.MailMessage;
import nl.mijnummah.identity.PasswordResetIT.CapturingMail;
import nl.mijnummah.identity.PasswordResetIT.CapturingMailer;

/** The platform administrator registers mosques with their first administrator, manages accounts, sees totals. */
@Import(CapturingMail.class)
class PlatformAdminIT extends AbstractIntegrationTest {

    private static final Pattern CODE = Pattern.compile("\\b([A-Z2-9]{4}-[A-Z2-9]{4})\\b");

    @Autowired
    private CapturingMailer mail;

    @BeforeEach
    void emptyInbox() {
        mail.sent.clear();
    }

    @Test
    void registeringAMosqueMakesASlugSettingsAndInvitesItsFirstAdministrator() {
        ApiResponse created = register(Map.of("details", Map.of("name", "Moskee Mijn-Ummah (Den Haag)"),
                "firstAdministrator", Map.of("email", "Bestuur@Example.com", "displayName", "Bestuur"),
                "settings", Map.of("registrationsEnabled", false, "pushEnabled", true)));
        assertThat(created.status()).as(created.body().toString()).isEqualTo(201);
        JsonNode mosque = created.body().get("mosque");
        assertThat(mosque.get("slug").asString()).isEqualTo("moskee-mijn-ummah-den-haag");
        assertThat(mosque.get("status").asString()).isEqualTo("ACTIVE");
        assertThat(created.body().get("settings").get("registrationsEnabled").asBoolean()).isFalse();
        assertThat(created.body().get("firstAdministrator").get("invited").asBoolean()).isTrue();
        UUID mosqueId = UUID.fromString(mosque.get("id").asString());

        MailMessage invitation = awaitEmail("bestuur@example.com");
        assertThat(invitation.subject()).isEqualTo("Je account bij Mijn Ummah");
        ApiResponse chosen = api.post("/api/v1/auth/password-reset/confirm", Map.of("email", "bestuur@example.com",
                "code", codeIn(invitation), "newPassword", "een eigen wachtwoord"));
        assertThat(chosen.status()).isEqualTo(200);
        String token = chosen.field("accessToken");
        assertThat(api.get("/api/v1/admin/mosques", token).body().findValuesAsString("myRole")).containsExactly("ADMIN");
        assertThat(api.get("/api/v1/admin/mosques/" + mosqueId + "/settings", token).body()
                .get("registrationsEnabled").asBoolean()).isFalse();

        ApiResponse sameName = register(Map.of("details", Map.of("name", "Moskee Mijn Ummah Den Haag")));
        assertThat(sameName.body().get("mosque").get("slug").asString()).isEqualTo("moskee-mijn-ummah-den-haag-2");
        assertThat(sameName.body().get("firstAdministrator").isNull()).isTrue();
    }

    @Test
    void anExistingAccountBecomesAdministratorWithoutEmailAndFailuresCreateNothing() {
        Account existing = user("bestaand");
        ApiResponse created = register(Map.of("slug", "al-fath", "details", Map.of("name", "Al Fath"),
                "firstAdministrator", Map.of("email", existing.email(), "displayName", "Wordt genegeerd")));
        assertThat(created.body().get("firstAdministrator").get("invited").asBoolean()).isFalse();
        assertThat(api.get("/api/v1/admin/mosques", existing.token()).body().size()).isEqualTo(1);

        Account blocked = user("geblokkeerd");
        api.post("/api/v1/admin/users/" + blocked.id() + "/block", null, platformAdmin.token());
        ApiResponse refused = register(Map.of("slug", "an-noor", "details", Map.of("name", "An Noor"),
                "firstAdministrator", Map.of("email", blocked.email(), "displayName", "X")));
        assertThat(refused.status()).isEqualTo(403);
        assertThat(refused.field("code")).isEqualTo("ACCOUNT_BLOCKED");
        assertThat(jdbc.queryForObject("select count(*) from mosque where slug = 'an-noor'", Integer.class))
                .as("rolled back").isZero();

        assertThat(api.post("/api/v1/admin/mosques", Map.of("details", Map.of("name", "X"),
                "firstAdministrator", Map.of("email", "geen-adres", "displayName", "X")), platformAdmin.token())
                .status()).isEqualTo(400);
    }

    @Test
    void onlyThePlatformAdminChangesSettingsAndTheyAreEnforced() {
        UUID mosqueId = mosque("as-sunnah");
        Account admin = mosqueAdmin(mosqueId, "admin");
        String path = adminPath(mosqueId, "settings");
        Map<String, Boolean> off = Map.of("registrationsEnabled", false, "pushEnabled", false);
        assertThat(api.put(path, off, admin.token()).status()).isEqualTo(403);
        assertThat(api.put(path, off, platformAdmin.token()).status()).isEqualTo(200);

        UUID event = publishedEvent(mosqueId, admin.token());
        jdbc.update("update event set registration_enabled = true where id = ?", event);
        ApiResponse registered = api.post("/api/v1/events/" + event + "/registrations", null, user("bezoeker").token());
        assertThat(registered.status()).isEqualTo(422);
        assertThat(registered.field("code")).isEqualTo("REGISTRATIONS_DISABLED_FOR_MOSQUE");
        assertThat(api.get(adminPath(mosqueId, "notifications/audience?contentType=EVENT"), admin.token()).body()
                .get("enabledForMosque").asBoolean()).isFalse();
        assertThat(jdbc.queryForList("select action from audit_log", String.class))
                .contains("MOSQUE_SETTINGS_UPDATED");
    }

    @Test
    void accountsAreSearchedInvitedAndSentAPasswordCode() {
        user("amina");
        Account yusuf = user("yusuf");
        ApiResponse found = api.get("/api/v1/admin/users?q=YUS", platformAdmin.token());
        assertThat(found.body().get("items").findValuesAsString("email")).containsExactly(yusuf.email());
        api.post("/api/v1/admin/users/" + yusuf.id() + "/block", null, platformAdmin.token());
        assertThat(api.get("/api/v1/admin/users?status=BLOCKED", platformAdmin.token()).body().get("items")
                .findValuesAsString("email")).containsExactly(yusuf.email());
        assertThat(api.get("/api/v1/admin/users?q=%25", platformAdmin.token()).body().get("totalItems").asLong())
                .as("a % is searched literally").isZero();

        ApiResponse invited = api.post("/api/v1/admin/users", Map.of("email", "nieuw@example.com",
                "displayName", "Nieuw", "platformRole", "USER"), platformAdmin.token());
        assertThat(invited.status()).isEqualTo(201);
        assertThat(invited.field("lastLoginAt")).isNull();
        awaitEmail("nieuw@example.com");

        Account amina = user("amina-2");
        assertThat(api.post("/api/v1/admin/users/" + amina.id() + "/password-code", null, amina.token()).status())
                .isEqualTo(403);
        assertThat(api.post("/api/v1/admin/users/" + amina.id() + "/password-code", null, platformAdmin.token())
                .status()).isEqualTo(204);
        assertThat(awaitEmail(amina.email()).subject()).isEqualTo("Je code om een nieuw wachtwoord in te stellen");
        assertThat(api.post("/api/v1/admin/users/" + yusuf.id() + "/password-code", null, platformAdmin.token())
                .field("code")).isEqualTo("ACCOUNT_BLOCKED");

        login(amina.email(), PASSWORD);
        ApiResponse listed = api.get("/api/v1/admin/users?q=amina-2", platformAdmin.token());
        assertThat(listed.body().get("items").get(0).get("lastLoginAt").isNull()).isFalse();
    }

    @Test
    void theDashboardCountsThePlatformForThePlatformAdminOnly() {
        UUID mosqueA = mosque("moskee-a");
        UUID mosqueB = mosque("moskee-b");
        Account admin = mosqueAdmin(mosqueA, "admin-a");
        publishedEvent(mosqueA, admin.token());
        api.patch("/api/v1/admin/mosques/" + mosqueB + "/status", Map.of("status", "BLOCKED"), platformAdmin.token());

        assertThat(api.get("/api/v1/admin/platform/dashboard", admin.token()).status()).isEqualTo(403);
        assertThat(api.get("/api/v1/admin/platform/mosques", admin.token()).status()).isEqualTo(403);

        JsonNode totals = api.get("/api/v1/admin/platform/dashboard", platformAdmin.token()).body();
        assertThat(totals.get("mosques").get("total").asLong()).isEqualTo(2);
        assertThat(totals.get("mosques").get("active").asLong()).isEqualTo(1);
        assertThat(totals.get("mosques").get("blocked").asLong()).isEqualTo(1);
        assertThat(totals.get("events").asLong()).isEqualTo(1);
        assertThat(totals.get("users").asLong()).isEqualTo(2);

        JsonNode perMosque = api.get("/api/v1/admin/platform/mosques", platformAdmin.token()).body();
        assertThat(perMosque.findValuesAsString("slug")).containsExactly("moskee-a", "moskee-b");
        assertThat(perMosque.get(0).get("events").asLong()).isEqualTo(1);
        assertThat(perMosque.get(0).get("administrators").asLong()).isEqualTo(1);
    }

    @Test
    void theAuditLogNamesTheMosque() {
        UUID mosqueId = mosque("al-huda");
        JsonNode entry = api.get("/api/v1/admin/audit-log?mosqueId=" + mosqueId, platformAdmin.token()).body()
                .get("items").get(0);
        assertThat(entry.get("mosqueName").asString()).isEqualTo("Moskee al-huda");
    }

    private ApiResponse register(Map<String, Object> body) {
        return api.post("/api/v1/admin/mosques", body, platformAdmin.token());
    }

    private MailMessage awaitEmail(String to) {
        await().atMost(Duration.ofSeconds(10)).until(() -> mail.sent.stream().anyMatch(m -> m.to().equals(to)));
        return mail.sent.stream().filter(m -> m.to().equals(to)).toList().getLast();
    }

    private static String codeIn(MailMessage email) {
        Matcher matcher = CODE.matcher(email.text());
        assertThat(matcher.find()).as("code in email").isTrue();
        return matcher.group(1);
    }
}
