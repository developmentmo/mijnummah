package nl.mijnummah.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;

import nl.mijnummah.AbstractIntegrationTest;
import nl.mijnummah.ApiTestClient.ApiResponse;
import nl.mijnummah.storage.TestImages;

/** The mosque portal (fase C): dashboard, images on content, archiving, publication dates, prayer times. */
class MosquePortalIT extends AbstractIntegrationTest {

    private UUID mosqueId;
    private UUID otherMosqueId;
    private Account admin;

    @BeforeEach
    void setUp() {
        mosqueId = mosque("al-fath");
        otherMosqueId = mosque("an-noor");
        admin = mosqueAdmin(mosqueId, "beheer");
    }

    @Test
    void theDashboardCountsOnlyThisMosque() {
        Account editor = user("redacteur");
        grant(mosqueId, editor, "EDITOR");
        Account follower = user("volger");
        api.put("/api/v1/me/follows/" + mosqueId, null, follower.token());
        api.put("/api/v1/me/follows/" + otherMosqueId, null, user("elders").token());
        publishedEvent(mosqueId, admin.token());
        draftEvent(mosqueId, eventBody("Concept", inDays(3)), admin.token());
        publishedEvent(otherMosqueId, mosqueAdmin(otherMosqueId, "ander").token());
        UUID scheduled = video("aaaaaaaaaaa");
        api.post(adminPath(mosqueId, "media/" + scheduled + "/publish"),
                Map.of("publishedAt", Instant.now().plus(Duration.ofDays(2)).toString()), admin.token());
        video("bbbbbbbbbbb");

        ApiResponse figures = api.get(adminPath(mosqueId, "dashboard"), editor.token());
        assertThat(figures.status()).isEqualTo(200);
        JsonNode f = figures.body();
        assertThat(f.get("followers").asLong()).isEqualTo(1);
        assertThat(f.get("newFollowers").asLong()).isEqualTo(1);
        assertThat(f.get("upcomingEvents").asLong()).isEqualTo(1);
        assertThat(f.get("draftEvents").asLong()).isEqualTo(1);
        assertThat(f.get("publishedVideos").asLong()).isZero();
        assertThat(f.get("scheduledVideos").asLong()).isEqualTo(1);
        assertThat(f.get("draftVideos").asLong()).isEqualTo(1);

        assertThat(api.get(adminPath(otherMosqueId, "dashboard"), admin.token()).status()).isEqualTo(403);
        assertThat(api.get(adminPath(mosqueId, "dashboard"), follower.token()).status()).isEqualTo(403);
    }

    @Test
    void contentUsesUploadedImagesOfTheSameMosqueOnly() {
        String image = upload(mosqueId, admin.token());
        String foreign = upload(otherMosqueId, mosqueAdmin(otherMosqueId, "ander").token());

        Map<String, Object> event = eventBody("Iftar", inDays(5));
        event.put("imageId", foreign);
        ApiResponse refused = api.post(adminPath(mosqueId, "events"), event, admin.token());
        assertThat(refused.status()).isEqualTo(404);
        assertThat(refused.field("code")).isEqualTo("IMAGE_NOT_FOUND");

        event.put("imageId", image);
        UUID eventId = publishedEvent(mosqueId, event, admin.token());
        ApiResponse publicEvent = api.get("/api/v1/public/mosques/al-fath/events/" + eventId);
        assertThat(publicEvent.field("imageUrl")).isEqualTo("/api/v1/public/images/" + image);

        ApiResponse announcement = api.post(adminPath(mosqueId, "announcements"),
                Map.of("title", "Nieuw dak", "body", "Het dak is klaar.", "imageId", image), admin.token());
        assertThat(announcement.status()).isEqualTo(201);
        String announcementId = announcement.field("id");
        api.post(adminPath(mosqueId, "announcements/" + announcementId + "/publish"), null, admin.token());
        assertThat(api.get("/api/v1/public/mosques/al-fath/announcements/" + announcementId).field("imageUrl"))
                .isEqualTo("/api/v1/public/images/" + image);

        Map<String, Object> media = videoBody("ccccccccccc");
        media.put("thumbnailImageId", image);
        ApiResponse withThumbnail = api.post(adminPath(mosqueId, "media"), media, admin.token());
        assertThat(withThumbnail.field("thumbnailUrl")).isEqualTo("/api/v1/public/images/" + image);
        assertThat(withThumbnail.field("thumbnailImageId")).isEqualTo(image);
    }

    @Test
    void anArchivedEventDisappearsFromTheAppAndCanBeRestored() {
        UUID eventId = publishedEvent(mosqueId, admin.token());
        String publicPath = "/api/v1/public/mosques/al-fath/events/" + eventId;

        ApiResponse draftArchive = api.post(adminPath(mosqueId, "events/"
                + draftEvent(mosqueId, eventBody("Concept", inDays(2)), admin.token()) + "/archive"), null, admin.token());
        assertThat(draftArchive.status()).isEqualTo(422);

        ApiResponse archived = api.post(adminPath(mosqueId, "events/" + eventId + "/archive"), null, admin.token());
        assertThat(archived.field("status")).isEqualTo("ARCHIVED");
        assertThat(archived.field("archivedAt")).isNotNull();
        assertThat(api.get(publicPath).status()).isEqualTo(404);
        assertThat(api.get("/api/v1/public/mosques/al-fath/events").body().get("items").size()).isZero();
        ApiResponse edit = api.put(adminPath(mosqueId, "events/" + eventId), eventBody("Anders", inDays(7)),
                admin.token());
        assertThat(edit.field("code")).isEqualTo("EVENT_ARCHIVED");
        assertThat(api.get(adminPath(mosqueId, "events?status=ARCHIVED"), admin.token()).body().get("items").size())
                .isEqualTo(1);

        ApiResponse restored = api.post(adminPath(mosqueId, "events/" + eventId + "/restore"), null, admin.token());
        assertThat(restored.field("status")).isEqualTo("PUBLISHED");
        assertThat(api.get(publicPath).status()).isEqualTo(200);
        assertThat(api.get("/api/v1/admin/audit-log?mosqueId=" + mosqueId, admin.token()).body()
                .findValuesAsString("action")).contains("EVENT_ARCHIVED", "EVENT_RESTORED");
    }

    @Test
    void aVideoAppearsInTheAppFromItsPublicationDate() {
        UUID videoId = video("ddddddddddd");
        String publish = adminPath(mosqueId, "media/" + videoId + "/publish");
        Instant tomorrow = Instant.now().plus(Duration.ofDays(1));

        ApiResponse scheduled = api.post(publish, Map.of("publishedAt", tomorrow.toString()), admin.token());
        assertThat(scheduled.field("status")).isEqualTo("PUBLISHED");
        assertThat(api.get("/api/v1/public/mosques/al-fath/media").body().get("items").size()).isZero();
        assertThat(api.get("/api/v1/public/mosques/al-fath/media/" + videoId).status()).isEqualTo(404);
        ApiResponse push = api.post(adminPath(mosqueId, "media/" + videoId + "/push"), null, admin.token());
        assertThat(push.field("code")).isEqualTo("CONTENT_NOT_PUBLISHED");

        ApiResponse tooFar = api.post(publish, Map.of("publishedAt", Instant.now().plus(Duration.ofDays(400))
                .toString()), admin.token());
        assertThat(tooFar.status()).isEqualTo(400);
        assertThat(tooFar.field("code")).isEqualTo("INVALID_PUBLICATION_DATE");

        Instant recorded = Instant.parse("2025-03-14T19:00:00Z");
        ApiResponse backdated = api.post(publish, Map.of("publishedAt", recorded.toString()), admin.token());
        assertThat(backdated.field("publishedAt")).isEqualTo(recorded.toString());
        ApiResponse list = api.get("/api/v1/public/mosques/al-fath/media");
        assertThat(list.body().get("items").size()).isEqualTo(1);

        // re-publishing without a date keeps the chosen one
        api.post(adminPath(mosqueId, "media/" + videoId + "/unpublish"), null, admin.token());
        assertThat(api.post(publish, null, admin.token()).field("publishedAt")).isEqualTo(recorded.toString());

        Map<String, Object> recording = videoBody("eeeeeeeeeee");
        recording.put("category", "RECORDED_EVENT");
        assertThat(api.post(adminPath(mosqueId, "media"), recording, admin.token()).status()).isEqualTo(201);
    }

    @Test
    void aMosqueLinksToItsPrayerTimes() {
        Map<String, Object> details = new HashMap<>(Map.of("name", "Al Fath",
                "prayerTimesUrl", "https://example.org/gebedstijden.pdf"));
        assertThat(api.put("/api/v1/admin/mosques/" + mosqueId, details, admin.token()).status()).isEqualTo(200);
        assertThat(api.get("/api/v1/public/mosques/al-fath").field("prayerTimesUrl"))
                .isEqualTo("https://example.org/gebedstijden.pdf");

        details.put("prayerTimesUrl", "javascript:alert(1)");
        assertThat(api.put("/api/v1/admin/mosques/" + mosqueId, details, admin.token()).status()).isEqualTo(400);
    }

    private String upload(UUID mosque, String token) {
        ApiResponse uploaded = api.upload(adminPath(mosque, "images"), "foto.png", TestImages.png(400, 300, false),
                token);
        assertThat(uploaded.status()).isEqualTo(201);
        return uploaded.field("id");
    }

    private UUID video(String youtubeId) {
        ApiResponse created = api.post(adminPath(mosqueId, "media"), videoBody(youtubeId), admin.token());
        assertThat(created.status()).as(created.body().toString()).isEqualTo(201);
        return UUID.fromString(created.field("id"));
    }

    private static Map<String, Object> videoBody(String youtubeId) {
        Map<String, Object> body = new HashMap<>();
        body.put("title", "Lezing " + youtubeId);
        body.put("category", "LECTURE");
        body.put("videoUrl", "https://www.youtube.com/watch?v=" + youtubeId);
        return body;
    }
}
