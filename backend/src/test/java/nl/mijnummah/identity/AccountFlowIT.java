package nl.mijnummah.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import nl.mijnummah.AbstractIntegrationTest;
import nl.mijnummah.ApiTestClient.ApiResponse;

/** Self-registration, changing the password and deleting the own account (fase 5). */
class AccountFlowIT extends AbstractIntegrationTest {

    private static final String NEW_PASSWORD = "een heel nieuw wachtwoord";

    @Test
    void registeringCreatesAPlainUserAndSignsIn() {
        ApiResponse registered = register("nieuw@example.com", "Amina", PASSWORD);
        assertThat(registered.status()).isEqualTo(201);
        assertThat(registered.field("refreshToken")).isNotBlank();

        ApiResponse me = api.get("/api/v1/auth/me", registered.field("accessToken"));
        assertThat(me.field("email")).isEqualTo("nieuw@example.com");
        assertThat(me.field("displayName")).isEqualTo("Amina");
        assertThat(me.field("platformRole")).isEqualTo("USER");

        // a fresh account manages nothing
        assertThat(api.get("/api/v1/admin/mosques", registered.field("accessToken")).body().size()).isZero();
    }

    @Test
    void registrationRejectsTakenAddressesWeakPasswordsAndInvalidInput() {
        register("dubbel@example.com", "Een", PASSWORD);
        ApiResponse taken = register("DUBBEL@example.com", "Twee", PASSWORD);
        assertThat(taken.status()).isEqualTo(409);
        assertThat(taken.field("code")).isEqualTo("EMAIL_ALREADY_REGISTERED");

        ApiResponse weak = register("zwak@example.com", "Zwak", "te kort");
        assertThat(weak.status()).isEqualTo(400);
        assertThat(weak.field("code")).isEqualTo("INVALID_PASSWORD");

        ApiResponse invalid = register("geen-adres", "", PASSWORD);
        assertThat(invalid.status()).isEqualTo(400);
        assertThat(invalid.field("code")).isEqualTo("VALIDATION_FAILED");
    }

    @Test
    void changingThePasswordEndsEverySessionAndReturnsANewOne() {
        Account account = user("wijzig");
        String oldRefresh = login(account.email(), PASSWORD).field("refreshToken");

        ApiResponse wrong = api.put("/api/v1/me/password",
                Map.of("currentPassword", "niet het wachtwoord", "newPassword", NEW_PASSWORD), account.token());
        assertThat(wrong.status()).isEqualTo(400);
        assertThat(wrong.field("code")).isEqualTo("CURRENT_PASSWORD_INCORRECT");

        ApiResponse changed = api.put("/api/v1/me/password",
                Map.of("currentPassword", PASSWORD, "newPassword", NEW_PASSWORD), account.token());
        assertThat(changed.status()).isEqualTo(200);
        assertThat(changed.field("refreshToken")).isNotBlank();

        assertThat(api.post("/api/v1/auth/refresh", Map.of("refreshToken", oldRefresh)).status()).isEqualTo(401);
        assertThat(api.post("/api/v1/auth/refresh", Map.of("refreshToken", changed.field("refreshToken"))).status())
                .isEqualTo(200);
        assertThat(api.post("/api/v1/auth/login", Map.of("email", account.email(), "password", PASSWORD)).status())
                .isEqualTo(401);
        login(account.email(), NEW_PASSWORD);
    }

    @Test
    void deletingTheAccountRemovesItsPersonalDataAndFreesItsPlaces() {
        UUID mosqueId = mosque("noeroel-islam");
        Account admin = mosqueAdmin(mosqueId, "beheer");
        Map<String, Object> body = new HashMap<>(eventBody("Lezing", inDays(5)));
        body.put("registrationEnabled", true);
        body.put("capacity", 1);
        UUID eventId = publishedEvent(mosqueId, body, admin.token());

        Account visitor = user("bezoeker");
        api.put("/api/v1/me/follows/" + mosqueId, null, visitor.token());
        api.put("/api/v1/me/follows/" + mosqueId + "/notifications", Map.of("events", false), visitor.token());
        api.put("/api/v1/me/devices", Map.of("token", "token-van-bezoeker", "platform", "ANDROID"), visitor.token());
        assertThat(api.post("/api/v1/events/" + eventId + "/registrations", null, visitor.token()).status())
                .isEqualTo(201);

        ApiResponse wrong = api.post("/api/v1/me/deletion", Map.of("password", "fout wachtwoord"), visitor.token());
        assertThat(wrong.status()).isEqualTo(400);
        assertThat(wrong.field("code")).isEqualTo("CURRENT_PASSWORD_INCORRECT");

        assertThat(api.post("/api/v1/me/deletion", Map.of("password", PASSWORD), visitor.token()).status())
                .isEqualTo(204);

        assertThat(api.post("/api/v1/auth/login", Map.of("email", visitor.email(), "password", PASSWORD)).status())
                .isEqualTo(401);
        assertThat(api.get("/api/v1/auth/me", visitor.token()).status()).isEqualTo(401);
        for (String table : new String[] {"mosque_follow", "notification_preference", "push_device",
                "event_registration", "refresh_token"}) {
            Integer rows = jdbc.queryForObject("select count(*) from " + table + " where user_id = ?", Integer.class,
                    visitor.id());
            assertThat(rows).as(table).isZero();
        }
        // the only place is free again
        assertThat(api.post("/api/v1/events/" + eventId + "/registrations", null, user("ander").token()).status())
                .isEqualTo(201);
    }

    @Test
    void administratorsCannotDeleteTheirOwnAccount() {
        UUID mosqueId = mosque("noeroel-islam");
        Account admin = mosqueAdmin(mosqueId, "beheer");

        ApiResponse blocked = api.post("/api/v1/me/deletion", Map.of("password", PASSWORD), admin.token());
        assertThat(blocked.status()).isEqualTo(409);
        assertThat(blocked.field("code")).isEqualTo("ACCOUNT_DELETION_BLOCKED");
        login(admin.email(), PASSWORD);
    }

    @Test
    void passwordResetIsUnavailableWithoutEmailForEveryAddressAlike() {
        Account account = user("vergeten");
        for (String email : new String[] {account.email(), "onbekend@example.com"}) {
            ApiResponse response = api.post("/api/v1/auth/password-reset", Map.of("email", email));
            assertThat(response.status()).isEqualTo(503);
            assertThat(response.field("code")).isEqualTo("PASSWORD_RESET_UNAVAILABLE");
        }
    }

    private ApiResponse register(String email, String displayName, String password) {
        return api.post("/api/v1/auth/register",
                Map.of("email", email, "displayName", displayName, "password", password));
    }
}
