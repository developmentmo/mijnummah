package nl.mijnummah.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import tools.jackson.databind.JsonNode;

import nl.mijnummah.AbstractIntegrationTest;
import nl.mijnummah.ApiTestClient.ApiResponse;

/**
 * Device registration and push notifications end to end, with the real FCM gateway against a local stand-in for
 * Google's endpoints ({@link FcmStub}). Covers who is addressed (mosque, preference, active account) and who may send.
 */
class PushFlowIT extends AbstractIntegrationTest {

    private static final FcmStub FCM = new FcmStub();

    @DynamicPropertySource
    static void fcm(DynamicPropertyRegistry registry) {
        registry.add("mijnummah.push.provider", () -> "fcm");
        registry.add("mijnummah.push.fcm.project-id", () -> FcmStub.PROJECT);
        registry.add("mijnummah.push.fcm.credentials-file", FCM::credentialsFile);
        registry.add("mijnummah.push.fcm.api-base-url", FCM::baseUrl);
    }

    private UUID mosqueId;
    private UUID otherMosqueId;
    private Account admin;

    @BeforeEach
    void setUp() {
        FCM.reset();
        mosqueId = mosque("noeroel-islam");
        otherMosqueId = mosque("andere-moskee");
        admin = mosqueAdmin(mosqueId, "beheer");
    }

    @Test
    void aDeviceBelongsToWhoeverIsSignedInOnIt() {
        Account first = user("eerste");
        Account second = user("tweede");
        assertThat(registerDevice(null, "gedeeld-toestel").status()).isEqualTo(401);
        assertThat(registerDevice(first, "gedeeld-toestel").status()).isEqualTo(204);
        assertThat(registerDevice(first, "gedeeld-toestel").status()).isEqualTo(204);
        assertThat(registerDevice(second, "gedeeld-toestel").status()).isEqualTo(204);
        assertThat(owners("gedeeld-toestel")).containsExactly(second.id());

        // the previous owner cannot remove it any more; the current one can
        api.post("/api/v1/me/devices/remove", Map.of("token", "gedeeld-toestel"), first.token());
        assertThat(owners("gedeeld-toestel")).containsExactly(second.id());
        assertThat(api.post("/api/v1/me/devices/remove", Map.of("token", "gedeeld-toestel"), second.token()).status())
                .isEqualTo(204);
        assertThat(owners("gedeeld-toestel")).isEmpty();

        ApiResponse invalid = api.put("/api/v1/me/devices",
                Map.of("token", "x", "platform", "WINDOWS", "mosqueSlug", "noeroel-islam"), first.token());
        assertThat(invalid.status()).isEqualTo(400);
        ApiResponse withoutApp = api.put("/api/v1/me/devices", Map.of("token", "x", "platform", "ANDROID"),
                first.token());
        assertThat(withoutApp.status()).isEqualTo(400);
        ApiResponse unknownApp = registerDevice(first, "x", "bestaat-niet");
        assertThat(unknownApp.status()).isEqualTo(404);
        assertThat(unknownApp.field("code")).isEqualTo("MOSQUE_NOT_FOUND");
        assertThat(owners("x")).isEmpty();
    }

    @Test
    void aNotificationOnlyReachesTheAppOfTheMosqueThatSentIt() {
        // one person follows both mosques and has both apps installed
        Account member = follower("beide-apps", Map.of());
        api.put("/api/v1/me/follows/" + otherMosqueId, null, member.token());
        registerDevice(member, "app-noeroel", "noeroel-islam");
        registerDevice(member, "app-andere", "andere-moskee");
        Account otherAdmin = mosqueAdmin(otherMosqueId, "ander-beheer");

        ApiResponse ownAudience = api.get(adminPath(mosqueId, "notifications/audience?contentType=ANNOUNCEMENT"),
                admin.token());
        assertThat(ownAudience.body().get("followers").asInt()).isEqualTo(1);
        assertThat(ownAudience.body().get("devices").asInt()).isEqualTo(1);
        ApiResponse otherAudience = api.get(adminPath(otherMosqueId,
                "notifications/audience?contentType=ANNOUNCEMENT"), otherAdmin.token());
        assertThat(otherAudience.body().get("devices").asInt()).isEqualTo(1);

        UUID announcementId = publishedAnnouncement("Alleen voor deze app", "Tekst");
        api.post(adminPath(mosqueId, "announcements/" + announcementId + "/push"), null, admin.token());
        awaitCompleted();
        assertThat(FCM.messages).extracting(m -> m.path("token").asString()).containsExactly("app-noeroel");

        // the token moves with the app it was registered from: re-registering it for the other app moves it there
        registerDevice(member, "app-noeroel", "andere-moskee");
        assertThat(jdbc.queryForObject("select mosque_id from push_device where token = ?", UUID.class,
                "app-noeroel")).isEqualTo(otherMosqueId);
    }

    @Test
    void onlyActiveFollowersOfThisMosqueWithTheTopicOnAreReached() {
        Audience people = audience();

        ApiResponse forAnnouncements = api.get(adminPath(mosqueId, "notifications/audience?contentType=ANNOUNCEMENT"),
                admin.token());
        assertThat(forAnnouncements.body().get("configured").asBoolean()).isTrue();
        assertThat(forAnnouncements.body().get("simulated").asBoolean()).isFalse();
        assertThat(forAnnouncements.body().get("followers").asInt()).isEqualTo(1);
        assertThat(forAnnouncements.body().get("devices").asInt()).isEqualTo(2);

        ApiResponse forEvents = api.get(adminPath(mosqueId, "notifications/audience?contentType=EVENT"),
                admin.token());
        assertThat(forEvents.body().get("followers").asInt()).isEqualTo(2);
        assertThat(forEvents.body().get("devices").asInt()).isEqualTo(3);

        UUID announcementId = publishedAnnouncement("Iftar vrijdag", "Iedereen is welkom bij de gezamenlijke iftar.");
        ApiResponse sent = api.post(adminPath(mosqueId, "announcements/" + announcementId + "/push"), null,
                admin.token());
        assertThat(sent.status()).isEqualTo(202);
        assertThat(sent.field("status")).isEqualTo("SENDING");
        assertThat(sent.body().get("recipientCount").asInt()).isEqualTo(2);

        ApiResponse done = awaitCompleted();
        assertThat(done.body().get("deliveredCount").asInt()).isEqualTo(2);
        assertThat(done.body().get("failedCount").asInt()).isZero();

        assertThat(FCM.messages).extracting(m -> m.path("token").asString())
                .containsExactlyInAnyOrder(people.wantsEverything() + "-telefoon", people.wantsEverything() + "-tablet");
        JsonNode message = FCM.messages.getFirst();
        assertThat(message.path("notification").path("title").asString()).isEqualTo("Iftar vrijdag");
        assertThat(message.path("notification").path("body").asString())
                .isEqualTo("Iedereen is welkom bij de gezamenlijke iftar.");
        assertThat(message.path("data").path("type").asString()).isEqualTo("announcement");
        assertThat(message.path("data").path("id").asString()).isEqualTo(announcementId.toString());

        // the real gateway signed an assertion, exchanged it and used the access token
        assertThat(FCM.assertions).isNotEmpty();
        assertThat(FCM.assertions.getFirst().getPayload().toJSONObject().get("scope"))
                .isEqualTo("https://www.googleapis.com/auth/firebase.messaging");
        assertThat(FCM.authorizations).allMatch(("Bearer " + FcmStub.ACCESS_TOKEN)::equals);
    }

    @Test
    void onlyAnAdminOfTheMosqueSendsAndOnlyOnceAboutPublishedContent() {
        Account editor = user("redacteur");
        grant(mosqueId, editor, "EDITOR");
        Account otherAdmin = mosqueAdmin(otherMosqueId, "ander-beheer");
        UUID announcementId = publishedAnnouncement("Les", "Tekst");
        String path = adminPath(mosqueId, "announcements/" + announcementId + "/push");

        assertThat(api.post(path, null, editor.token()).status()).isEqualTo(403);
        assertThat(api.post(path, null, otherAdmin.token()).status()).isEqualTo(403);
        assertThat(api.post(adminPath(otherMosqueId, "announcements/" + announcementId + "/push"), null,
                otherAdmin.token()).status()).isEqualTo(404);

        ApiResponse draft = api.post(adminPath(mosqueId, "announcements/" + draftAnnouncement() + "/push"), null,
                admin.token());
        assertThat(draft.status()).isEqualTo(422);
        assertThat(draft.field("code")).isEqualTo("CONTENT_NOT_PUBLISHED");

        ApiResponse tooLong = api.post(path, Map.of("title", "x".repeat(101)), admin.token());
        assertThat(tooLong.status()).isEqualTo(400);

        ApiResponse sent = api.post(path, Map.of("title", "Eigen titel", "body", "Eigen tekst"), admin.token());
        assertThat(sent.status()).isEqualTo(202);
        assertThat(sent.field("title")).isEqualTo("Eigen titel");
        assertThat(sent.field("body")).isEqualTo("Eigen tekst");
        assertThat(sent.field("status")).as("nobody follows yet").isEqualTo("SENT");

        ApiResponse again = api.post(path, null, admin.token());
        assertThat(again.status()).isEqualTo(409);
        assertThat(again.field("code")).isEqualTo("PUSH_ALREADY_SENT");

        ApiResponse forEditor = api.get(adminPath(mosqueId, "notifications?contentType=ANNOUNCEMENT&contentId="
                + announcementId), editor.token());
        assertThat(forEditor.body().get("items").size()).isEqualTo(1);
        assertThat(api.get(adminPath(mosqueId, "notifications"), otherAdmin.token()).status()).isEqualTo(403);
    }

    @Test
    void devicesWhoseAppIsGoneAreCountedAsFailedAndRemoved() {
        Account follower = follower("volger", Map.of());
        registerDevice(follower, "dead-oude-telefoon");
        registerDevice(follower, "volger-nieuwe-telefoon");

        UUID announcementId = publishedAnnouncement("Nieuws", "Tekst");
        api.post(adminPath(mosqueId, "announcements/" + announcementId + "/push"), null, admin.token());

        ApiResponse done = awaitCompleted();
        assertThat(done.body().get("deliveredCount").asInt()).isEqualTo(1);
        assertThat(done.body().get("failedCount").asInt()).isEqualTo(1);
        assertThat(owners("dead-oude-telefoon")).isEmpty();
        assertThat(owners("volger-nieuwe-telefoon")).containsExactly(follower.id());
    }

    @Test
    void eventsAndVideosHaveTheirOwnDefaultText() {
        Account follower = follower("volger", Map.of());
        registerDevice(follower, "volger-telefoon");

        UUID eventId = publishedEvent(mosqueId, admin.token());
        ApiResponse event = api.post(adminPath(mosqueId, "events/" + eventId + "/push"), null, admin.token());
        assertThat(event.status()).isEqualTo(202);
        assertThat(event.field("title")).isEqualTo("Lezing");
        assertThat(event.field("body")).matches("[a-z]{2} \\d{1,2} [a-z]{3}, \\d{2}:\\d{2} · Grote zaal");

        Map<String, Object> video = new HashMap<>();
        video.put("title", "Geduld in moeilijke tijden");
        video.put("videoUrl", "https://www.youtube.com/watch?v=dQw4w9WgXcQ");
        video.put("category", "LECTURE");
        video.put("speaker", "Imam Yusuf");
        ApiResponse created = api.post(adminPath(mosqueId, "media"), video, admin.token());
        String mediaId = created.field("id");
        api.post(adminPath(mosqueId, "media/" + mediaId + "/publish"), null, admin.token());
        ApiResponse media = api.post(adminPath(mosqueId, "media/" + mediaId + "/push"), null, admin.token());
        assertThat(media.status()).isEqualTo(202);
        assertThat(media.field("body")).isEqualTo("Lezing van Imam Yusuf");

        await().atMost(Duration.ofSeconds(10)).until(() -> FCM.messages.size() == 2);
        assertThat(FCM.messages).extracting(m -> m.path("data").path("type").asString())
                .containsExactlyInAnyOrder("event", "media");

        api.post(adminPath(mosqueId, "events/" + eventId + "/cancel"), null, admin.token());
        UUID other = publishedEvent(mosqueId, admin.token());
        api.post(adminPath(mosqueId, "events/" + other + "/cancel"), null, admin.token());
        assertThat(api.post(adminPath(mosqueId, "events/" + other + "/push"), null, admin.token()).field("code"))
                .isEqualTo("CONTENT_NOT_PUBLISHED");
    }

    /** Followers of this mosque in every combination that matters, each with devices. */
    private Audience audience() {
        Account wantsEverything = follower("alles", Map.of());
        registerDevice(wantsEverything, tokenPrefix(wantsEverything) + "-telefoon");
        registerDevice(wantsEverything, tokenPrefix(wantsEverything) + "-tablet");

        Account noAnnouncements = follower("geen-aankondigingen", Map.of("announcements", false));
        registerDevice(noAnnouncements, "geen-aankondigingen-telefoon");

        Account blocked = follower("geblokkeerd", Map.of());
        registerDevice(blocked, "geblokkeerd-telefoon");
        api.post("/api/v1/admin/users/" + blocked.id() + "/block", null, platformAdmin.token());

        Account elsewhere = user("elders");
        api.put("/api/v1/me/follows/" + otherMosqueId, null, elsewhere.token());
        registerDevice(elsewhere, "elders-telefoon");

        registerDevice(user("volgt-niet"), "volgt-niet-telefoon");
        return new Audience(tokenPrefix(wantsEverything));
    }

    private record Audience(String wantsEverything) {
    }

    private static String tokenPrefix(Account account) {
        return "alles-" + account.id();
    }

    private Account follower(String name, Map<String, Object> preferences) {
        Account account = user(name);
        api.put("/api/v1/me/follows/" + mosqueId, null, account.token());
        if (!preferences.isEmpty()) {
            api.put("/api/v1/me/follows/" + mosqueId + "/notifications", preferences, account.token());
        }
        return account;
    }

    /** Registers the token from the app of noeroel-islam. */
    private ApiResponse registerDevice(Account account, String token) {
        return registerDevice(account, token, "noeroel-islam");
    }

    private ApiResponse registerDevice(Account account, String token, String appMosqueSlug) {
        return api.put("/api/v1/me/devices", Map.of("token", token, "platform", "ANDROID", "mosqueSlug", appMosqueSlug),
                account == null ? null : account.token());
    }

    private List<UUID> owners(String token) {
        return jdbc.queryForList("select user_id from push_device where token = ?", UUID.class, token);
    }

    private UUID publishedAnnouncement(String title, String body) {
        UUID id = UUID.fromString(api.post(adminPath(mosqueId, "announcements"), Map.of("title", title, "body", body),
                admin.token()).field("id"));
        assertThat(api.post(adminPath(mosqueId, "announcements/" + id + "/publish"), null, admin.token()).status())
                .isEqualTo(200);
        return id;
    }

    private UUID draftAnnouncement() {
        return UUID.fromString(api.post(adminPath(mosqueId, "announcements"), Map.of("title", "Concept", "body", "x"),
                admin.token()).field("id"));
    }

    private ApiResponse awaitCompleted() {
        String path = adminPath(mosqueId, "notifications");
        await().atMost(Duration.ofSeconds(10)).until(() ->
                "SENT".equals(api.get(path, admin.token()).body().get("items").get(0).path("status").asString()));
        ApiResponse page = api.get(path, admin.token());
        return new ApiResponse(page.status(), page.headers(), page.body().get("items").get(0));
    }
}
