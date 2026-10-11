package nl.mijnummah.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import nl.mijnummah.AbstractIntegrationTest;
import nl.mijnummah.ApiTestClient.ApiResponse;

class AuthFlowIT extends AbstractIntegrationTest {

    @Test
    void loginReturnsTokensAndMeShowsTheAccount() {
        Account user = user("amina");
        ApiResponse login = login(user.email().toUpperCase(), PASSWORD);
        assertThat(login.field("tokenType")).isEqualTo("Bearer");
        assertThat(login.field("refreshToken")).hasSize(43);

        ApiResponse me = api.get("/api/v1/auth/me", login.field("accessToken"));
        assertThat(me.status()).isEqualTo(200);
        assertThat(me.field("email")).isEqualTo(user.email());
        assertThat(me.field("platformRole")).isEqualTo("USER");
        assertThat(me.body().has("passwordHash")).isFalse();
    }

    @Test
    void wrongPasswordAndUnknownEmailGetTheSameAnswer() {
        Account user = user("bilal");
        ApiResponse wrong = api.post("/api/v1/auth/login", Map.of("email", user.email(), "password", "wrong password!"));
        ApiResponse unknown = api.post("/api/v1/auth/login",
                Map.of("email", "nobody-" + UUID.randomUUID() + "@example.com", "password", "wrong password!"));
        assertThat(wrong.status()).isEqualTo(401);
        assertThat(unknown.status()).isEqualTo(401);
        assertThat(wrong.field("code")).isEqualTo("INVALID_CREDENTIALS").isEqualTo(unknown.field("code"));
        assertThat(wrong.field("message")).isEqualTo(unknown.field("message"));
    }

    @Test
    void protectedEndpointsRequireAValidToken() {
        assertThat(api.get("/api/v1/auth/me").status()).isEqualTo(401);
        ApiResponse forged = api.get("/api/v1/auth/me", "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ4In0.invalid");
        assertThat(forged.status()).isEqualTo(401);
        assertThat(forged.field("code")).isEqualTo("UNAUTHORIZED");
        assertThat(forged.headers().firstValue("WWW-Authenticate")).contains("Bearer");
    }

    @Test
    void refreshRotatesAndReuseOfAnOldTokenEndsTheSession() {
        Account user = user("chadia");
        ApiResponse login = login(user.email(), PASSWORD);
        String first = login.field("refreshToken");

        ApiResponse rotated = api.post("/api/v1/auth/refresh", Map.of("refreshToken", first));
        assertThat(rotated.status()).isEqualTo(200);
        String second = rotated.field("refreshToken");
        assertThat(second).isNotEqualTo(first);

        // replaying the first token: rejected, and the whole family (incl. the second token) is revoked
        assertThat(api.post("/api/v1/auth/refresh", Map.of("refreshToken", first)).status()).isEqualTo(401);
        ApiResponse afterReuse = api.post("/api/v1/auth/refresh", Map.of("refreshToken", second));
        assertThat(afterReuse.status()).isEqualTo(401);
        assertThat(afterReuse.field("code")).isEqualTo("INVALID_REFRESH_TOKEN");
    }

    @Test
    void logoutRevokesTheRefreshToken() {
        Account user = user("dawud");
        String refresh = login(user.email(), PASSWORD).field("refreshToken");
        assertThat(api.post("/api/v1/auth/logout", Map.of("refreshToken", refresh)).status()).isEqualTo(204);
        assertThat(api.post("/api/v1/auth/refresh", Map.of("refreshToken", refresh)).status()).isEqualTo(401);
        // idempotent, also for unknown tokens
        assertThat(api.post("/api/v1/auth/logout", Map.of("refreshToken", "unknown")).status()).isEqualTo(204);
    }

    @Test
    void blockingAUserStopsTheirTokensImmediately() {
        Account user = user("emre");
        String refresh = login(user.email(), PASSWORD).field("refreshToken");

        ApiResponse blocked = api.post("/api/v1/admin/users/" + user.id() + "/block", null, platformAdmin.token());
        assertThat(blocked.status()).isEqualTo(200);
        assertThat(blocked.field("status")).isEqualTo("BLOCKED");

        assertThat(api.get("/api/v1/auth/me", user.token()).status()).isEqualTo(401);
        assertThat(api.post("/api/v1/auth/refresh", Map.of("refreshToken", refresh)).status()).isEqualTo(401);
        ApiResponse login = api.post("/api/v1/auth/login", Map.of("email", user.email(), "password", PASSWORD));
        assertThat(login.status()).isEqualTo(403);
        assertThat(login.field("code")).isEqualTo("ACCOUNT_BLOCKED");

        api.post("/api/v1/admin/users/" + user.id() + "/unblock", null, platformAdmin.token());
        assertThat(api.post("/api/v1/auth/login", Map.of("email", user.email(), "password", PASSWORD)).status())
                .isEqualTo(200);
    }

    @Test
    void repeatedFailedLoginsForOneAccountAreThrottled() {
        Account user = user("fatima");
        for (int i = 0; i < 10; i++) {
            api.post("/api/v1/auth/login", Map.of("email", user.email(), "password", "wrong password!"));
        }
        ApiResponse throttled = api.post("/api/v1/auth/login", Map.of("email", user.email(), "password", PASSWORD));
        assertThat(throttled.status()).isEqualTo(429);
        assertThat(throttled.field("code")).isEqualTo("TOO_MANY_LOGIN_ATTEMPTS");
        assertThat(throttled.headers().firstValue("Retry-After")).isPresent();
    }

    @Test
    void onlyAPlatformAdminManagesAccounts() {
        Account user = user("goran");
        Map<String, Object> body = Map.of("email", "new-" + UUID.randomUUID() + "@example.com",
                "displayName", "Nieuw", "password", PASSWORD, "platformRole", "USER");

        ApiResponse forbidden = api.post("/api/v1/admin/users", body, user.token());
        assertThat(forbidden.status()).isEqualTo(403);
        assertThat(api.get("/api/v1/admin/users", user.token()).status()).isEqualTo(403);

        ApiResponse created = api.post("/api/v1/admin/users", body, platformAdmin.token());
        assertThat(created.status()).isEqualTo(201);
        assertThat(api.post("/api/v1/admin/users", body, platformAdmin.token()).field("code"))
                .isEqualTo("EMAIL_ALREADY_REGISTERED");

        ApiResponse weak = api.post("/api/v1/admin/users", Map.of("email", "weak@example.com", "displayName", "Zwak",
                "password", "short", "platformRole", "USER"), platformAdmin.token());
        assertThat(weak.status()).isEqualTo(400);
        assertThat(weak.field("code")).isEqualTo("INVALID_PASSWORD");

        ApiResponse self = api.post("/api/v1/admin/users/" + platformAdmin.id() + "/block", null, platformAdmin.token());
        assertThat(self.status()).isEqualTo(422);
    }
}
