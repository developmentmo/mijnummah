package nl.mijnummah.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nl.mijnummah.AbstractIntegrationTest;
import nl.mijnummah.ApiTestClient.ApiResponse;

class EventFlowIT extends AbstractIntegrationTest {

    private static final String PUBLIC = "/api/v1/public/mosques/noeroel-islam/events";

    private UUID mosqueId;
    private Account admin;
    private Account editor;

    @BeforeEach
    void mosqueWithStaff() {
        mosqueId = mosque("noeroel-islam");
        admin = mosqueAdmin(mosqueId, "admin");
        editor = user("editor");
        grant(mosqueId, editor, "EDITOR");
    }

    @Test
    void draftsAreInvisibleUntilPublished() {
        UUID id = draftEvent(mosqueId, eventBody("Tafsir", inDays(2)), editor.token());
        assertThat(api.get(PUBLIC).body().get("totalItems").asLong()).isZero();
        assertThat(api.get(PUBLIC + "/" + id).status()).isEqualTo(404);

        ApiResponse published = api.post(adminPath(mosqueId, "events/" + id + "/publish"), null, editor.token());
        assertThat(published.field("status")).isEqualTo("PUBLISHED");
        assertThat(published.field("publishedAt")).isNotNull();

        ApiResponse detail = api.get(PUBLIC + "/" + id);
        assertThat(detail.status()).isEqualTo(200);
        assertThat(detail.field("title")).isEqualTo("Tafsir");
        assertThat(detail.body().get("registration").get("registeredByMe").isNull()).isTrue();
        assertThat(detail.body().has("createdBy")).isFalse();
    }

    @Test
    void publicAgendaIsUpcomingSortedAndFilterable() {
        publishedEvent(mosqueId, eventBody("Later", inDays(10)), admin.token());
        Map<String, Object> soon = eventBody("Binnenkort", inDays(1));
        soon.put("category", "YOUTH");
        publishedEvent(mosqueId, soon, admin.token());
        publishedEvent(mosqueId, eventBody("Voorbij", inDays(-5)), admin.token());

        ApiResponse upcoming = api.get(PUBLIC);
        assertThat(upcoming.body().get("totalItems").asLong()).isEqualTo(2);
        assertThat(upcoming.body().get("items").get(0).get("title").asString()).isEqualTo("Binnenkort");
        assertThat(upcoming.body().get("items").get(1).get("title").asString()).isEqualTo("Later");

        assertThat(api.get(PUBLIC + "?category=YOUTH").body().get("totalItems").asLong()).isEqualTo(1);
        assertThat(api.get(PUBLIC + "?from=" + inDays(-30)).body().get("totalItems").asLong()).isEqualTo(3);
        assertThat(api.get(PUBLIC + "?to=" + inDays(5)).body().get("totalItems").asLong()).isEqualTo(1);
        assertThat(api.get(PUBLIC + "?size=1").body().get("totalPages").asInt()).isEqualTo(2);
        assertThat(api.get(PUBLIC + "?category=NONSENSE").status()).isEqualTo(400);
    }

    @Test
    void registeredUsersOnlyEventsAreHiddenFromAnonymousVisitors() {
        Map<String, Object> body = eventBody("Ledenavond", inDays(3));
        body.put("visibility", "REGISTERED_USERS");
        UUID id = publishedEvent(mosqueId, body, admin.token());

        assertThat(api.get(PUBLIC).body().get("totalItems").asLong()).isZero();
        assertThat(api.get(PUBLIC + "/" + id).status()).isEqualTo(404);
        Account member = user("lid");
        assertThat(api.get(PUBLIC, member.token()).body().get("totalItems").asLong()).isEqualTo(1);
        assertThat(api.get(PUBLIC + "/" + id, member.token()).status()).isEqualTo(200);
    }

    @Test
    void cancelledEventsStayVisibleWithTheirStatusAndCannotBeChanged() {
        UUID id = publishedEvent(mosqueId, admin.token());
        ApiResponse cancelled = api.post(adminPath(mosqueId, "events/" + id + "/cancel"), null, admin.token());
        assertThat(cancelled.field("status")).isEqualTo("CANCELLED");
        assertThat(api.get(PUBLIC + "/" + id).field("status")).isEqualTo("CANCELLED");

        ApiResponse edit = api.put(adminPath(mosqueId, "events/" + id), eventBody("Toch", inDays(4)), admin.token());
        assertThat(edit.status()).isEqualTo(422);
        assertThat(edit.field("code")).isEqualTo("EVENT_CANCELLED");
    }

    @Test
    void invalidInputIsRejectedWithFieldErrors() {
        Map<String, Object> body = eventBody("", inDays(2));
        body.put("capacity", 0);
        body.put("imageUrl", "http://insecure.example/a.png");
        ApiResponse invalid = api.post(adminPath(mosqueId, "events"), body, admin.token());
        assertThat(invalid.status()).isEqualTo(400);
        assertThat(invalid.field("code")).isEqualTo("VALIDATION_FAILED");
        assertThat(invalid.body().get("fieldErrors").toString()).contains("title", "capacity", "imageUrl");

        Map<String, Object> backwards = eventBody("Achterstevoren", inDays(2));
        backwards.put("endsAt", inDays(1).toString());
        ApiResponse period = api.post(adminPath(mosqueId, "events"), backwards, admin.token());
        assertThat(period.status()).isEqualTo(400);
        assertThat(period.field("code")).isEqualTo("INVALID_EVENT");

        assertThat(api.post(adminPath(mosqueId, "events"), Map.of("title", "Zonder categorie en tijd"), admin.token())
                .body().get("fieldErrors").toString()).contains("category", "startsAt");
    }

    @Test
    void onlyDraftsWithoutRegistrationsCanBeDeleted() {
        UUID draft = draftEvent(mosqueId, eventBody("Concept", inDays(2)), admin.token());
        UUID published = publishedEvent(mosqueId, admin.token());
        ApiResponse deletePublished = api.delete(adminPath(mosqueId, "events/" + published), admin.token());
        assertThat(deletePublished.status()).isEqualTo(422);
        assertThat(deletePublished.field("code")).isEqualTo("EVENT_NOT_DRAFT");
        assertThat(api.delete(adminPath(mosqueId, "events/" + draft), admin.token()).status()).isEqualTo(204);
        assertThat(api.get(adminPath(mosqueId, "events/" + draft), admin.token()).status()).isEqualTo(404);
    }

    @Test
    void adminListShowsDraftsAndFiltersByStatus() {
        draftEvent(mosqueId, eventBody("Concept", inDays(2)), admin.token());
        publishedEvent(mosqueId, admin.token());
        assertThat(api.get(adminPath(mosqueId, "events"), editor.token()).body().get("totalItems").asLong()).isEqualTo(2);
        assertThat(api.get(adminPath(mosqueId, "events?status=DRAFT"), editor.token()).body().get("totalItems").asLong())
                .isEqualTo(1);
        assertThat(api.get(adminPath(mosqueId, "events"), user("buitenstaander").token()).status()).isEqualTo(403);
        assertThat(api.get(adminPath(mosqueId, "events")).status()).isEqualTo(401);
    }

    // ---------- registrations

    @Test
    void registrationNeedsAnAccount() {
        UUID id = publishedEvent(mosqueId, registrable("Cursus", inDays(5), null), admin.token());
        assertThat(api.post("/api/v1/events/" + id + "/registrations", null).status()).isEqualTo(401);
    }

    @Test
    void userRegistersOnceAndCanCancelAndRegisterAgain() {
        UUID id = publishedEvent(mosqueId, registrable("Cursus", inDays(5), null), admin.token());
        Account visitor = user("bezoeker");
        String path = "/api/v1/events/" + id + "/registrations";

        ApiResponse registered = api.post(path, null, visitor.token());
        assertThat(registered.status()).isEqualTo(201);
        assertThat(registered.field("status")).isEqualTo("CONFIRMED");
        assertThat(registered.field("mosqueSlug")).isEqualTo("noeroel-islam");

        ApiResponse duplicate = api.post(path, null, visitor.token());
        assertThat(duplicate.status()).isEqualTo(409);
        assertThat(duplicate.field("code")).isEqualTo("ALREADY_REGISTERED");

        assertThat(api.get(PUBLIC + "/" + id, visitor.token()).body().get("registration").get("registeredByMe")
                .asBoolean()).isTrue();
        assertThat(api.get("/api/v1/me/event-registrations", visitor.token()).body().get("totalItems").asLong())
                .isEqualTo(1);

        assertThat(api.delete(path + "/me", visitor.token()).status()).isEqualTo(204);
        assertThat(api.delete(path + "/me", visitor.token()).field("code")).isEqualTo("REGISTRATION_NOT_FOUND");
        assertThat(api.get("/api/v1/me/event-registrations", visitor.token()).body().get("totalItems").asLong()).isZero();

        assertThat(api.post(path, null, visitor.token()).status()).isEqualTo(201);
        Integer rows = jdbc.queryForObject("select count(*) from event_registration where event_id = ?", Integer.class, id);
        assertThat(rows).isEqualTo(1);
    }

    @Test
    void capacityIsNeverExceeded() {
        UUID id = publishedEvent(mosqueId, registrable("Kleine groep", inDays(5), 2), admin.token());
        String path = "/api/v1/events/" + id + "/registrations";
        assertThat(api.post(path, null, user("een").token()).status()).isEqualTo(201);
        assertThat(api.post(path, null, user("twee").token()).status()).isEqualTo(201);
        ApiResponse full = api.post(path, null, user("drie").token());
        assertThat(full.status()).isEqualTo(409);
        assertThat(full.field("code")).isEqualTo("EVENT_FULL");
        assertThat(api.get(PUBLIC + "/" + id).body().get("registration").get("availablePlaces").asInt()).isZero();

        ApiResponse lowered = api.put(adminPath(mosqueId, "events/" + id), registrable("Kleine groep", inDays(5), 1),
                admin.token());
        assertThat(lowered.status()).isEqualTo(422);
        assertThat(lowered.field("code")).isEqualTo("CAPACITY_BELOW_REGISTRATIONS");
    }

    @Test
    void registrationRulesFollowTheEventState() {
        Account visitor = user("bezoeker");
        UUID noRegistration = publishedEvent(mosqueId, eventBody("Vrije inloop", inDays(3)), admin.token());
        assertThat(api.post("/api/v1/events/" + noRegistration + "/registrations", null, visitor.token())
                .field("code")).isEqualTo("REGISTRATION_NOT_ENABLED");

        UUID started = publishedEvent(mosqueId, registrable("Al begonnen", Instant.now().minusSeconds(600), null),
                admin.token());
        assertThat(api.post("/api/v1/events/" + started + "/registrations", null, visitor.token()).field("code"))
                .isEqualTo("REGISTRATION_CLOSED");

        UUID cancelled = publishedEvent(mosqueId, registrable("Gaat niet door", inDays(3), null), admin.token());
        api.post(adminPath(mosqueId, "events/" + cancelled + "/cancel"), null, admin.token());
        ApiResponse onCancelled = api.post("/api/v1/events/" + cancelled + "/registrations", null, visitor.token());
        assertThat(onCancelled.status()).isEqualTo(422);
        assertThat(onCancelled.field("code")).isEqualTo("EVENT_CANCELLED");

        UUID draft = draftEvent(mosqueId, registrable("Concept", inDays(3), null), admin.token());
        assertThat(api.post("/api/v1/events/" + draft + "/registrations", null, visitor.token()).status()).isEqualTo(404);
        assertThat(api.post("/api/v1/events/" + UUID.randomUUID() + "/registrations", null, visitor.token()).status())
                .isEqualTo(404);
    }

    @Test
    void onlyAdminsSeeWhoRegisteredAndPublishedEventsWithRegistrationsCannotBeUnpublished() {
        UUID id = publishedEvent(mosqueId, registrable("Cursus", inDays(5), null), admin.token());
        Account visitor = user("bezoeker");
        api.post("/api/v1/events/" + id + "/registrations", null, visitor.token());

        ApiResponse list = api.get(adminPath(mosqueId, "events/" + id + "/registrations"), admin.token());
        assertThat(list.status()).isEqualTo(200);
        assertThat(list.body().size()).isEqualTo(1);
        assertThat(list.body().get(0).get("email").asString()).isEqualTo(visitor.email());
        assertThat(api.get(adminPath(mosqueId, "events/" + id + "/registrations"), editor.token()).status())
                .isEqualTo(403);

        ApiResponse unpublish = api.post(adminPath(mosqueId, "events/" + id + "/unpublish"), null, admin.token());
        assertThat(unpublish.status()).isEqualTo(409);
        assertThat(unpublish.field("code")).isEqualTo("EVENT_HAS_REGISTRATIONS");
        assertThat(api.get(adminPath(mosqueId, "events/" + id), admin.token()).body().get("confirmedRegistrations")
                .asLong()).isEqualTo(1);
    }

    private Map<String, Object> registrable(String title, Instant startsAt, Integer capacity) {
        Map<String, Object> body = eventBody(title, startsAt);
        body.put("registrationEnabled", true);
        if (capacity != null) {
            body.put("capacity", capacity);
        }
        return body;
    }
}
