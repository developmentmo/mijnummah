package nl.mijnummah.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;

import nl.mijnummah.AbstractIntegrationTest;
import nl.mijnummah.ApiTestClient.ApiResponse;

/** The central app (fase D): discovering mosques, content of all mosques, favourites. */
class CentralAppIT extends AbstractIntegrationTest {

    private UUID alFath;
    private UUID anNoor;
    private UUID closed;
    private Account fathAdmin;
    private Account noorAdmin;

    @BeforeEach
    void setUp() {
        alFath = mosque("al-fath");
        anNoor = mosque("an-noor");
        closed = mosque("gesloten");
        rename(anNoor, "Moskee An-Noor", "Amsterdam");
        rename(closed, "Moskee Gesloten", "Rotterdam");
        fathAdmin = mosqueAdmin(alFath, "fath");
        noorAdmin = mosqueAdmin(anNoor, "noor");
    }

    @Test
    void visitorsDiscoverActiveMosquesBySearchAndPlace() {
        close();
        JsonNode all = api.get("/api/v1/public/mosques").body();
        assertThat(slugs(all)).containsExactly("al-fath", "an-noor");
        assertThat(all.get("totalItems").asInt()).isEqualTo(2);

        assertThat(slugs(api.get("/api/v1/public/mosques?q=NOOR").body())).containsExactly("an-noor");
        assertThat(slugs(api.get("/api/v1/public/mosques?q=%25").body())).isEmpty();
        assertThat(slugs(api.get("/api/v1/public/mosques?city=amsterdam").body())).containsExactly("an-noor");
        assertThat(slugs(api.get("/api/v1/public/mosques?city=Rotterdam").body())).isEmpty();
        assertThat(api.get("/api/v1/public/mosques/cities").body().valueStream().map(JsonNode::asString))
                .containsExactly("Amsterdam", "Utrecht");
    }

    @Test
    void theAgendaShowsEventsOfAllActiveMosquesWithTheirMosque() {
        UUID fathEvent = publishedEvent(alFath, eventBody("Lezing Al Fath", inDays(2)), fathAdmin.token());
        UUID noorEvent = publishedEvent(anNoor, eventBody("Lezing An-Noor", inDays(3)), noorAdmin.token());
        Map<String, Object> restricted = eventBody("Alleen leden", inDays(4));
        restricted.put("visibility", "REGISTERED_USERS");
        publishedEvent(anNoor, restricted, noorAdmin.token());
        draftEvent(alFath, eventBody("Concept", inDays(1)), fathAdmin.token());
        publishedEvent(closed, eventBody("Gesloten moskee", inDays(1)), platformAdmin.token());
        close();

        JsonNode anonymous = api.get("/api/v1/public/events").body().get("items");
        assertThat(titles(anonymous)).containsExactly("Lezing Al Fath", "Lezing An-Noor");
        assertThat(anonymous.get(0).path("mosque").path("slug").asString()).isEqualTo("al-fath");
        assertThat(anonymous.get(1).path("mosque").path("name").asString()).isEqualTo("Moskee An-Noor");

        String token = user("bezoeker").token();
        assertThat(titles(api.get("/api/v1/public/events", token).body().get("items")))
                .containsExactly("Lezing Al Fath", "Lezing An-Noor", "Alleen leden");
        assertThat(titles(api.get("/api/v1/public/events?mosque=an-noor&mosque=gesloten&mosque=onbekend", token)
                .body().get("items"))).containsExactly("Lezing An-Noor", "Alleen leden");
        assertThat(api.get("/api/v1/public/events?mosque=onbekend").body().get("items").size()).isZero();

        ApiResponse one = api.get("/api/v1/public/events/" + noorEvent);
        assertThat(one.body().path("mosque").path("id").asString()).isEqualTo(anNoor.toString());
        assertThat(one.body().path("registration").path("enabled").asBoolean()).isFalse();
        assertThat(api.get("/api/v1/public/events/" + fathEvent).status()).isEqualTo(200);

        // when the platform switches registrations off for a mosque, its events say so
        Map<String, Object> withRegistration = eventBody("Met aanmelden", inDays(5));
        withRegistration.put("registrationEnabled", true);
        UUID open = publishedEvent(alFath, withRegistration, fathAdmin.token());
        assertThat(api.get("/api/v1/public/events/" + open).body().path("registration").path("enabled")
                .asBoolean()).isTrue();
        api.put("/api/v1/admin/mosques/" + alFath + "/settings",
                Map.of("registrationsEnabled", false, "pushEnabled", true), platformAdmin.token());
        assertThat(api.get("/api/v1/public/events/" + open).body().path("registration").path("enabled")
                .asBoolean()).isFalse();
        assertThat(api.get("/api/v1/public/mosques/al-fath/events").body().get("items").get(0)
                .path("registration").path("enabled").asBoolean()).isFalse();
    }

    @Test
    void contentOfAnInactiveMosqueOrNotPublishedIsNotFoundById() {
        UUID event = publishedEvent(closed, eventBody("Gesloten", inDays(1)), platformAdmin.token());
        UUID video = video(closed, "aaaaaaaaaaa", platformAdmin.token());
        close();
        assertThat(api.get("/api/v1/public/events/" + event).status()).isEqualTo(404);
        UUID draft = draftEvent(alFath, eventBody("Concept", inDays(1)), fathAdmin.token());
        assertThat(api.get("/api/v1/public/events/" + draft).status()).isEqualTo(404);
        assertThat(api.get("/api/v1/public/media/" + video).status()).isEqualTo(404);
        assertThat(api.get("/api/v1/public/media").body().get("items").size()).isZero();
    }

    @Test
    void videosAndNewsOfAllMosquesCanBeFilteredByMosque() {
        UUID fathVideo = video(alFath, "bbbbbbbbbbb", fathAdmin.token());
        video(anNoor, "ccccccccccc", noorAdmin.token());
        JsonNode videos = api.get("/api/v1/public/media").body().get("items");
        assertThat(videos.size()).isEqualTo(2);
        assertThat(api.get("/api/v1/public/media?mosque=al-fath").body().get("items").get(0).path("mosque")
                .path("slug").asString()).isEqualTo("al-fath");
        assertThat(api.get("/api/v1/public/media?q=ccccc").body().get("items").size()).isEqualTo(1);
        assertThat(api.get("/api/v1/public/media/" + fathVideo).body().path("mosque").path("name").asString())
                .isEqualTo("Moskee al-fath");
        assertThat(api.get("/api/v1/public/media/" + fathVideo + "/related").status()).isEqualTo(200);

        String fathNews = announcement(alFath, "Nieuws Al Fath", fathAdmin.token());
        announcement(anNoor, "Nieuws An-Noor", noorAdmin.token());
        assertThat(titles(api.get("/api/v1/public/announcements").body().get("items")))
                .containsExactly("Nieuws An-Noor", "Nieuws Al Fath");
        assertThat(titles(api.get("/api/v1/public/announcements?mosque=al-fath").body().get("items")))
                .containsExactly("Nieuws Al Fath");
        assertThat(api.get("/api/v1/public/announcements/" + fathNews).body().path("mosque").path("slug")
                .asString()).isEqualTo("al-fath");
    }

    @Test
    void aUserKeepsFavouriteMosquesAndVideos() {
        close();
        Account member = user("lid");
        api.put("/api/v1/me/follows/" + alFath, null, member.token());
        ApiResponse favourite = api.put("/api/v1/me/follows/" + anNoor + "/favorite", null, member.token());
        assertThat(favourite.status()).isEqualTo(200);
        assertThat(favourite.body().path("favorite").asBoolean()).isTrue();

        JsonNode follows = api.get("/api/v1/me/follows", member.token()).body();
        assertThat(follows.valueStream().map(f -> f.path("mosqueSlug").asString()))
                .containsExactly("an-noor", "al-fath");
        assertThat(api.delete("/api/v1/me/follows/" + anNoor + "/favorite", member.token()).body().path("favorite")
                .asBoolean()).isFalse();
        assertThat(api.get("/api/v1/me/follows", member.token()).body().size()).isEqualTo(2);
        assertThat(api.delete("/api/v1/me/follows/" + closed + "/favorite", member.token()).status()).isEqualTo(404);

        UUID video = video(alFath, "ddddddddddd", fathAdmin.token());
        UUID draft = draftVideo(alFath, "eeeeeeeeeee", fathAdmin.token());
        assertThat(api.put("/api/v1/me/favorite-media/" + video, null, member.token()).status()).isEqualTo(204);
        assertThat(api.put("/api/v1/me/favorite-media/" + video, null, member.token()).status()).isEqualTo(204);
        assertThat(api.put("/api/v1/me/favorite-media/" + draft, null, member.token()).status()).isEqualTo(404);
        assertThat(api.put("/api/v1/me/favorite-media/" + video, null, null).status()).isEqualTo(401);

        JsonNode saved = api.get("/api/v1/me/favorite-media", member.token()).body().get("items");
        assertThat(saved.size()).isEqualTo(1);
        assertThat(saved.get(0).path("mosque").path("slug").asString()).isEqualTo("al-fath");
        assertThat(api.get("/api/v1/me/favorite-media/ids", member.token()).body().get(0).asString())
                .isEqualTo(video.toString());
        assertThat(api.get("/api/v1/me/favorite-media", user("ander").token()).body().get("items").size()).isZero();

        // unpublishing hides it from the list; removing works also then
        api.post(adminPath(alFath, "media/" + video + "/unpublish"), null, fathAdmin.token());
        assertThat(api.get("/api/v1/me/favorite-media", member.token()).body().get("items").size()).isZero();
        assertThat(api.delete("/api/v1/me/favorite-media/" + video, member.token()).status()).isEqualTo(204);
        assertThat(api.get("/api/v1/me/favorite-media/ids", member.token()).body().size()).isZero();
    }

    /** Makes the third mosque inactive: hidden from the app. */
    private void close() {
        assertThat(api.patch("/api/v1/admin/mosques/" + closed + "/status", Map.of("status", "INACTIVE"),
                platformAdmin.token()).status()).isEqualTo(200);
    }

    private void rename(UUID mosqueId, String name, String city) {
        ApiResponse updated = api.put("/api/v1/admin/mosques/" + mosqueId, Map.of("name", name, "city", city),
                platformAdmin.token());
        assertThat(updated.status()).as(updated.body().toString()).isEqualTo(200);
    }

    private UUID draftVideo(UUID mosqueId, String youtubeId, String token) {
        Map<String, Object> body = new HashMap<>();
        body.put("title", "Lezing " + youtubeId);
        body.put("category", "LECTURE");
        body.put("videoUrl", "https://www.youtube.com/watch?v=" + youtubeId);
        ApiResponse created = api.post(adminPath(mosqueId, "media"), body, token);
        assertThat(created.status()).as(created.body().toString()).isEqualTo(201);
        return UUID.fromString(created.field("id"));
    }

    private UUID video(UUID mosqueId, String youtubeId, String token) {
        UUID id = draftVideo(mosqueId, youtubeId, token);
        assertThat(api.post(adminPath(mosqueId, "media/" + id + "/publish"), null, token).status()).isEqualTo(200);
        return id;
    }

    private String announcement(UUID mosqueId, String title, String token) {
        String id = api.post(adminPath(mosqueId, "announcements"), Map.of("title", title, "body", "Tekst"), token)
                .field("id");
        api.post(adminPath(mosqueId, "announcements/" + id + "/publish"), null, token);
        return id;
    }

    private static List<String> slugs(JsonNode page) {
        return page.get("items").valueStream().map(m -> m.path("slug").asString()).toList();
    }

    private static List<String> titles(JsonNode items) {
        return items.valueStream().map(m -> m.path("title").asString()).toList();
    }
}
