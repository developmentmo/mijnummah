package nl.mijnummah;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

import tools.jackson.databind.json.JsonMapper;

import nl.mijnummah.ApiTestClient.ApiResponse;
import nl.mijnummah.identity.PlatformRole;
import nl.mijnummah.identity.application.UserAdminService;

/**
 * Base for integration tests: an empty database before every test, an HTTP client against the real port, and
 * helpers that create accounts, mosques and content through the public API (as clients would).
 */
@IntegrationTest
public abstract class AbstractIntegrationTest {

    protected static final String PASSWORD = "correct horse battery staple";

    @LocalServerPort
    private int port;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    private UserAdminService userAdminService;

    protected ApiTestClient api;

    /** Platform administrator, created fresh for every test. */
    protected Account platformAdmin;

    @BeforeEach
    void resetDatabase() {
        jdbc.execute("""
                TRUNCATE push_notification, push_device, password_reset_code, notification_preference, mosque_follow,
                         announcement, media_item, event_registration, event, mosque_administrator, mosque,
                         refresh_token, app_user""");
        api = new ApiTestClient(port, jsonMapper);
        platformAdmin = account("platform", PlatformRole.PLATFORM_ADMIN);
    }

    /** Creates an account (as the platform admin would) and logs in. */
    protected Account account(String name, PlatformRole role) {
        String email = name + "-" + UUID.randomUUID() + "@example.com";
        UUID id = userAdminService.createUnchecked(email, name, PASSWORD, role).id();
        return new Account(id, email, login(email, PASSWORD).field("accessToken"));
    }

    protected Account user(String name) {
        return account(name, PlatformRole.USER);
    }

    protected ApiResponse login(String email, String password) {
        ApiResponse response = api.post("/api/v1/auth/login", Map.of("email", email, "password", password));
        assertThat(response.status()).as("login %s", email).isEqualTo(200);
        return response;
    }

    /** Creates a mosque as platform admin and returns its ID. */
    protected UUID mosque(String slug) {
        ApiResponse created = api.post("/api/v1/admin/mosques",
                Map.of("slug", slug, "details", Map.of("name", "Moskee " + slug, "city", "Utrecht")),
                platformAdmin.token());
        assertThat(created.status()).as("create mosque %s: %s", slug, created.body()).isEqualTo(201);
        return UUID.fromString(created.field("id"));
    }

    /** Gives an existing account a role (ADMIN or EDITOR) for a mosque. */
    protected void grant(UUID mosqueId, Account account, String role) {
        ApiResponse granted = api.post("/api/v1/admin/mosques/" + mosqueId + "/administrators",
                Map.of("email", account.email(), "role", role), platformAdmin.token());
        assertThat(granted.status()).as("grant %s: %s", role, granted.body()).isEqualTo(201);
    }

    protected Account mosqueAdmin(UUID mosqueId, String name) {
        Account account = user(name);
        grant(mosqueId, account, "ADMIN");
        return account;
    }

    protected Map<String, Object> eventBody(String title, Instant startsAt) {
        Map<String, Object> body = new HashMap<>();
        body.put("title", title);
        body.put("category", "LECTURE");
        body.put("startsAt", startsAt.toString());
        body.put("endsAt", startsAt.plus(Duration.ofHours(2)).toString());
        body.put("location", "Grote zaal");
        return body;
    }

    /** Creates a draft event and returns its ID. */
    protected UUID draftEvent(UUID mosqueId, Map<String, Object> body, String token) {
        ApiResponse created = api.post(adminPath(mosqueId, "events"), body, token);
        assertThat(created.status()).as("create event: %s", created.body()).isEqualTo(201);
        return UUID.fromString(created.field("id"));
    }

    /** Creates and publishes an event starting in a week; returns its ID. */
    protected UUID publishedEvent(UUID mosqueId, String token) {
        return publishedEvent(mosqueId, eventBody("Lezing", inDays(7)), token);
    }

    protected UUID publishedEvent(UUID mosqueId, Map<String, Object> body, String token) {
        UUID id = draftEvent(mosqueId, body, token);
        ApiResponse published = api.post(adminPath(mosqueId, "events/" + id + "/publish"), null, token);
        assertThat(published.status()).as("publish event: %s", published.body()).isEqualTo(200);
        return id;
    }

    protected static String adminPath(UUID mosqueId, String rest) {
        return "/api/v1/admin/mosques/" + mosqueId + "/" + rest;
    }

    protected static Instant inDays(int days) {
        return Instant.now().plus(days, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
    }

    public record Account(UUID id, String email, String token) {
    }
}
