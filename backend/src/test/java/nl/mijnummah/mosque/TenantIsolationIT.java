package nl.mijnummah.mosque;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nl.mijnummah.AbstractIntegrationTest;
import nl.mijnummah.ApiTestClient.ApiResponse;

/**
 * The administrator of mosque A can read or change nothing of mosque B: not through B's routes (403), and not by
 * putting B's IDs in A's routes (404). Public routes never mix content of mosques either.
 */
class TenantIsolationIT extends AbstractIntegrationTest {

    private UUID mosqueA;
    private UUID mosqueB;
    private Account adminA;
    private Account adminB;
    private UUID eventB;
    private UUID mediaB;
    private UUID announcementB;

    @BeforeEach
    void twoMosques() {
        mosqueA = mosque("moskee-a");
        mosqueB = mosque("moskee-b");
        adminA = mosqueAdmin(mosqueA, "admin-a");
        adminB = mosqueAdmin(mosqueB, "admin-b");
        eventB = publishedEvent(mosqueB, adminB.token());
        mediaB = UUID.fromString(api.post(adminPath(mosqueB, "media"), Map.of("title", "Lezing B", "category", "LECTURE",
                "videoUrl", "https://youtu.be/dQw4w9WgXcQ"), adminB.token()).field("id"));
        announcementB = UUID.fromString(api.post(adminPath(mosqueB, "announcements"),
                Map.of("title", "Nieuws B", "body", "Alleen voor B"), adminB.token()).field("id"));
    }

    @Test
    void adminOfACannotUseTheRoutesOfB() {
        String token = adminA.token();
        List<ApiResponse> attempts = List.of(
                api.get("/api/v1/admin/mosques/" + mosqueB, token),
                api.put("/api/v1/admin/mosques/" + mosqueB, Map.of("name", "Overgenomen"), token),
                api.get(adminPath(mosqueB, "administrators"), token),
                api.post(adminPath(mosqueB, "administrators"), Map.of("email", adminA.email(), "role", "ADMIN"), token),
                api.get(adminPath(mosqueB, "events"), token),
                api.post(adminPath(mosqueB, "events"), eventBody("Stiekem", inDays(3)), token),
                api.get(adminPath(mosqueB, "events/" + eventB), token),
                api.put(adminPath(mosqueB, "events/" + eventB), eventBody("Gewijzigd", inDays(3)), token),
                api.post(adminPath(mosqueB, "events/" + eventB + "/cancel"), null, token),
                api.delete(adminPath(mosqueB, "events/" + eventB), token),
                api.get(adminPath(mosqueB, "events/" + eventB + "/registrations"), token),
                api.get(adminPath(mosqueB, "media"), token),
                api.get(adminPath(mosqueB, "media/" + mediaB), token),
                api.post(adminPath(mosqueB, "media/" + mediaB + "/publish"), null, token),
                api.delete(adminPath(mosqueB, "media/" + mediaB), token),
                api.get(adminPath(mosqueB, "announcements"), token),
                api.get(adminPath(mosqueB, "announcements/" + announcementB), token),
                api.post(adminPath(mosqueB, "announcements/" + announcementB + "/publish"), null, token),
                api.delete(adminPath(mosqueB, "announcements/" + announcementB), token));
        for (ApiResponse attempt : attempts) {
            assertThat(attempt.status()).as(attempt.body().toString()).isEqualTo(403);
            assertThat(attempt.field("code")).isEqualTo("MOSQUE_ACCESS_DENIED");
        }
    }

    @Test
    void idsOfBInTheRoutesOfAAreNotFound() {
        String token = adminA.token();
        List<ApiResponse> attempts = List.of(
                api.get(adminPath(mosqueA, "events/" + eventB), token),
                api.put(adminPath(mosqueA, "events/" + eventB), eventBody("Gewijzigd", inDays(3)), token),
                api.post(adminPath(mosqueA, "events/" + eventB + "/cancel"), null, token),
                api.delete(adminPath(mosqueA, "events/" + eventB), token),
                api.get(adminPath(mosqueA, "events/" + eventB + "/registrations"), token),
                api.get(adminPath(mosqueA, "media/" + mediaB), token),
                api.put(adminPath(mosqueA, "media/" + mediaB), Map.of("title", "X", "category", "LECTURE",
                        "videoUrl", "https://youtu.be/dQw4w9WgXcQ"), token),
                api.delete(adminPath(mosqueA, "media/" + mediaB), token),
                api.get(adminPath(mosqueA, "announcements/" + announcementB), token),
                api.post(adminPath(mosqueA, "announcements/" + announcementB + "/publish"), null, token),
                api.delete(adminPath(mosqueA, "announcements/" + announcementB), token));
        for (ApiResponse attempt : attempts) {
            assertThat(attempt.status()).as(attempt.body().toString()).isEqualTo(404);
        }
        // nothing of B changed
        ApiResponse event = api.get(adminPath(mosqueB, "events/" + eventB), adminB.token());
        assertThat(event.field("status")).isEqualTo("PUBLISHED");
        assertThat(event.field("title")).isEqualTo("Lezing");
        assertThat(api.get(adminPath(mosqueB, "media/" + mediaB), adminB.token()).status()).isEqualTo(200);
        assertThat(api.get(adminPath(mosqueB, "announcements/" + announcementB), adminB.token()).field("status"))
                .isEqualTo("DRAFT");
    }

    @Test
    void anAdministratorIdOfBCannotBeRemovedThroughA() {
        String rowOfB = api.get(adminPath(mosqueB, "administrators"), adminB.token()).body().get(0).get("id").asString();
        ApiResponse attempt = api.delete(adminPath(mosqueA, "administrators/" + rowOfB), adminA.token());
        assertThat(attempt.status()).isEqualTo(404);
        assertThat(api.get(adminPath(mosqueB, "administrators"), adminB.token()).body().size()).isEqualTo(1);
    }

    @Test
    void relatedEventMustBelongToTheSameMosque() {
        ApiResponse attempt = api.post(adminPath(mosqueA, "media"), Map.of("title", "Lezing A", "category", "LECTURE",
                "videoUrl", "https://youtu.be/aaaaaaaaaaa", "relatedEventId", eventB.toString()), adminA.token());
        assertThat(attempt.status()).isEqualTo(400);
        assertThat(attempt.field("code")).isEqualTo("INVALID_RELATED_EVENT");
    }

    @Test
    void publicRoutesOfANeverShowContentOfB() {
        assertThat(api.get("/api/v1/public/mosques/moskee-a/events/" + eventB).status()).isEqualTo(404);
        assertThat(api.get("/api/v1/public/mosques/moskee-a/events").body().get("totalItems").asLong()).isZero();
        assertThat(api.get("/api/v1/public/mosques/moskee-b/events").body().get("totalItems").asLong()).isEqualTo(1);
        assertThat(api.get("/api/v1/public/mosques/moskee-a/media/" + mediaB).status()).isEqualTo(404);
    }

    @Test
    void theSameVideoMayBeUsedByTwoMosques() {
        ApiResponse created = api.post(adminPath(mosqueA, "media"), Map.of("title", "Lezing A", "category", "LECTURE",
                "videoUrl", "https://youtu.be/dQw4w9WgXcQ"), adminA.token());
        assertThat(created.status()).isEqualTo(201);
    }
}
