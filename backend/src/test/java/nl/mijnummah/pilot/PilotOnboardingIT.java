package nl.mijnummah.pilot;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;

import nl.mijnummah.AbstractIntegrationTest;
import nl.mijnummah.ApiTestClient.ApiResponse;
import nl.mijnummah.storage.TestImages;

/**
 * Fase E: a mosque joins the platform entirely through the API the portals use, with no code, configuration or
 * database change; a second mosque joins the same way. Each step is what the testing guide (TESTHANDLEIDING.md)
 * asks a tester to do by hand.
 */
class PilotOnboardingIT extends AbstractIntegrationTest {

    private record Pilot(UUID mosqueId, String slug, Account admin, Account editor) {
    }

    @Test
    void aMosqueJoinsThePlatformEntirelyThroughThePortal() {
        Pilot pilot = onboard("Testmoskee Al-Amal", "Utrecht");

        // the app shows the mosque with its own identity and content, also to anonymous visitors
        JsonNode listed = api.get("/api/v1/public/mosques?q=al-amal").body().get("items");
        assertThat(listed.size()).isEqualTo(1);
        assertThat(listed.get(0).path("logoUrl").asString()).startsWith("/api/v1/public/images/");
        JsonNode profile = api.get("/api/v1/public/mosques/" + pilot.slug()).body();
        assertThat(profile.path("coverUrl").asString()).startsWith("/api/v1/public/images/");
        assertThat(profile.path("prayerTimesUrl").asString()).isEqualTo("https://example.org/gebedstijden");
        assertThat(profile.path("address").path("city").asString()).isEqualTo("Utrecht");
        assertThat(titles("/api/v1/public/events?mosque=" + pilot.slug())).containsExactly("Kennismakingsavond");
        assertThat(titles("/api/v1/public/media?mosque=" + pilot.slug())).containsExactly("Lezing over geduld");
        assertThat(titles("/api/v1/public/announcements?mosque=" + pilot.slug())).containsExactly("Welkom in de app");

        // a visitor makes an account, follows the mosque and saves a video
        Account visitor = user("bezoeker");
        assertThat(api.put("/api/v1/me/follows/" + pilot.mosqueId(), null, visitor.token()).status()).isEqualTo(200);
        String video = api.get("/api/v1/public/media?mosque=" + pilot.slug()).body().get("items").get(0).path("id")
                .asString();
        assertThat(api.put("/api/v1/me/favorite-media/" + video, null, visitor.token()).status()).isEqualTo(204);

        // the mosque portal shows the follower; the audit log tells who set the mosque up
        ApiResponse dashboard = api.get(adminPath(pilot.mosqueId(), "dashboard"), pilot.admin().token());
        assertThat(dashboard.status()).as(dashboard.body().toString()).isEqualTo(200);
        assertThat(dashboard.body().toString()).contains("\"followers\"");
        List<String> actions = api.get("/api/v1/admin/audit-log?mosqueId=" + pilot.mosqueId(), pilot.admin().token())
                .body().get("items").findValuesAsString("action");
        assertThat(actions).contains("MOSQUE_CREATED", "MOSQUE_UPDATED", "MOSQUE_IMAGES_UPDATED");
    }

    @Test
    void aSecondMosqueJoinsTheSameWayAndTheTwoStaySeparate() {
        Pilot first = onboard("Testmoskee Al-Amal", "Utrecht");
        Pilot second = onboard("Testmoskee As-Salam", "Rotterdam");

        assertThat(api.get("/api/v1/public/mosques").body().get("items").findValuesAsString("slug"))
                .containsExactly(first.slug(), second.slug());
        assertThat(api.get("/api/v1/public/mosques/cities").body().valueStream().map(JsonNode::asString))
                .containsExactly("Rotterdam", "Utrecht");
        assertThat(titles("/api/v1/public/events")).containsExactly("Kennismakingsavond", "Kennismakingsavond");
        assertThat(api.get("/api/v1/public/events").body().get("items").findValues("mosque").stream()
                .map(m -> m.path("slug").asString())).containsExactlyInAnyOrder(first.slug(), second.slug());

        for (Pilot pilot : List.of(first, second)) {
            Pilot other = pilot == first ? second : first;
            for (Account account : List.of(pilot.admin(), pilot.editor())) {
                assertThat(api.get("/api/v1/admin/mosques", account.token()).body().findValuesAsString("slug"))
                        .as("mosques of %s", account.email()).containsExactly(pilot.slug());
                assertThat(api.get(adminPath(other.mosqueId(), "events"), account.token()).status()).isEqualTo(403);
                assertThat(api.get(adminPath(other.mosqueId(), "dashboard"), account.token()).status())
                        .isEqualTo(403);
            }
        }
        assertThat(api.get("/api/v1/admin/platform/dashboard", platformAdmin.token()).body().toString())
                .contains("\"mosques\"");
    }

    /**
     * What a tester does in the portals: the future mosque administrator makes an account in the app, the platform
     * admin registers the mosque with that person as first administrator, and the administrator then sets up the
     * profile, images, an editor and the first content.
     */
    private Pilot onboard(String name, String city) {
        Account admin = registerInApp("beheerder " + city);
        ApiResponse created = api.post("/api/v1/admin/mosques", Map.of(
                "details", Map.of("name", name, "city", city),
                "firstAdministrator", Map.of("email", admin.email(), "displayName", "Beheerder"),
                "settings", Map.of("registrationsEnabled", true, "pushEnabled", true)), platformAdmin.token());
        assertThat(created.status()).as(created.body().toString()).isEqualTo(201);
        assertThat(created.body().path("firstAdministrator").path("invited").asBoolean())
                .as("an existing account is not invited again").isFalse();
        UUID mosqueId = UUID.fromString(created.body().path("mosque").path("id").asString());
        String slug = created.body().path("mosque").path("slug").asString();
        assertThat(slug).as("the slug is made from the name").isEqualTo(
                name.toLowerCase().replace(' ', '-'));

        // profile and images, by the mosque's own administrator
        Map<String, Object> details = new HashMap<>();
        details.put("name", name);
        details.put("city", city);
        details.put("description", "Een moskee voor de hele wijk.");
        details.put("prayerTimesUrl", "https://example.org/gebedstijden");
        details.put("email", "info@example.org");
        ApiResponse updated = api.put("/api/v1/admin/mosques/" + mosqueId, details, admin.token());
        assertThat(updated.status()).as(updated.body().toString()).isEqualTo(200);
        String logo = upload(mosqueId, "logo.png", TestImages.png(512, 512, true), admin);
        String cover = upload(mosqueId, "omslag.jpg", TestImages.jpeg(1600, 900), admin);
        ApiResponse images = api.put("/api/v1/admin/mosques/" + mosqueId + "/images",
                Map.of("logoImageId", logo, "coverImageId", cover), admin.token());
        assertThat(images.status()).as(images.body().toString()).isEqualTo(200);

        // an editor joins and publishes content; the administrator posts the news
        Account editor = registerInApp("redacteur " + city);
        ApiResponse added = api.post(adminPath(mosqueId, "administrators"),
                Map.of("email", editor.email(), "role", "EDITOR"), admin.token());
        assertThat(added.status()).as(added.body().toString()).isEqualTo(201);
        publishedEvent(mosqueId, eventBody("Kennismakingsavond", inDays(5)), editor.token());
        String media = api.post(adminPath(mosqueId, "media"), Map.of("title", "Lezing over geduld",
                "category", "LECTURE", "videoUrl", "https://www.youtube.com/watch?v=dQw4w9WgXcQ"), editor.token())
                .field("id");
        assertThat(api.post(adminPath(mosqueId, "media/" + media + "/publish"), null, editor.token()).status())
                .isEqualTo(200);
        String news = api.post(adminPath(mosqueId, "announcements"),
                Map.of("title", "Welkom in de app", "body", "Vanaf nu vind je ons ook in Mijn Ummah."), admin.token())
                .field("id");
        assertThat(api.post(adminPath(mosqueId, "announcements/" + news + "/publish"), null, admin.token()).status())
                .isEqualTo(200);
        return new Pilot(mosqueId, slug, admin, editor);
    }

    /** Registers through the app's own sign-up, as a real future administrator would. */
    private Account registerInApp(String name) {
        String email = name.replace(' ', '.') + "-" + UUID.randomUUID() + "@example.com";
        ApiResponse registered = api.post("/api/v1/auth/register",
                Map.of("email", email, "displayName", name, "password", PASSWORD));
        assertThat(registered.status()).as(registered.body().toString()).isEqualTo(201);
        String token = login(email, PASSWORD).field("accessToken");
        return new Account(UUID.fromString(api.get("/api/v1/auth/me", token).field("id")), email, token);
    }

    private String upload(UUID mosqueId, String fileName, byte[] content, Account admin) {
        ApiResponse uploaded = api.upload("/api/v1/admin/mosques/" + mosqueId + "/images", fileName, content,
                admin.token());
        assertThat(uploaded.status()).as(uploaded.body().toString()).isEqualTo(201);
        return uploaded.field("id");
    }

    private List<String> titles(String path) {
        return api.get(path).body().get("items").valueStream().map(item -> item.path("title").asString()).toList();
    }
}
