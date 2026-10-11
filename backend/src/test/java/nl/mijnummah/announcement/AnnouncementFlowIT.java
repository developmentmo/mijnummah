package nl.mijnummah.announcement;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nl.mijnummah.AbstractIntegrationTest;
import nl.mijnummah.ApiTestClient.ApiResponse;

class AnnouncementFlowIT extends AbstractIntegrationTest {

    private static final String PUBLIC = "/api/v1/public/mosques/noeroel-islam/announcements";

    private UUID mosqueId;
    private Account editor;

    @BeforeEach
    void mosqueWithEditor() {
        mosqueId = mosque("noeroel-islam");
        mosqueAdmin(mosqueId, "admin");
        editor = user("editor");
        grant(mosqueId, editor, "EDITOR");
    }

    @Test
    void publishedAnnouncementsAreShownFeaturedFirstThenNewest() throws InterruptedException {
        String draft = create("Concept", false, null);
        String older = publish(create("Ouder nieuws", false, null));
        Thread.sleep(5);
        String newer = publish(create("Nieuwer nieuws", false, null));
        String featured = publish(create("Uitgelicht", true, null));

        ApiResponse list = api.get(PUBLIC);
        assertThat(list.body().get("totalItems").asLong()).isEqualTo(3);
        var items = list.body().get("items");
        assertThat(items.get(0).get("id").asString()).isEqualTo(featured);
        assertThat(items.get(1).get("id").asString()).isEqualTo(newer);
        assertThat(items.get(2).get("id").asString()).isEqualTo(older);
        assertThat(api.get(PUBLIC + "/" + draft).status()).isEqualTo(404);
        assertThat(api.get(PUBLIC + "/" + older).field("body")).isEqualTo("Tekst van Ouder nieuws");
    }

    @Test
    void expiredAnnouncementsDisappearAndCannotBePublished() {
        String id = publish(create("Kort geldig", false, Instant.now().plusSeconds(3600)));
        assertThat(api.get(PUBLIC + "/" + id).status()).isEqualTo(200);

        jdbc.update("update announcement set expires_at = now() - interval '1 minute' where id = ?::uuid", id);
        assertThat(api.get(PUBLIC + "/" + id).status()).isEqualTo(404);
        assertThat(api.get(PUBLIC).body().get("totalItems").asLong()).isZero();
        assertThat(api.get(adminPath(mosqueId, "announcements/" + id), editor.token()).body().get("expired").asBoolean())
                .isTrue();

        String expired = create("Al verlopen", false, Instant.now().minusSeconds(60));
        ApiResponse attempt = api.post(adminPath(mosqueId, "announcements/" + expired + "/publish"), null, editor.token());
        assertThat(attempt.status()).isEqualTo(400);
        assertThat(attempt.field("code")).isEqualTo("INVALID_ANNOUNCEMENT");
    }

    @Test
    void editorUpdatesUnpublishesAndDeletes() {
        String id = publish(create("Eerste versie", false, null));
        ApiResponse updated = api.put(adminPath(mosqueId, "announcements/" + id),
                Map.of("title", "Tweede versie", "body", "Bijgewerkt"), editor.token());
        assertThat(updated.field("title")).isEqualTo("Tweede versie");
        assertThat(api.get(PUBLIC + "/" + id).field("title")).isEqualTo("Tweede versie");

        api.post(adminPath(mosqueId, "announcements/" + id + "/unpublish"), null, editor.token());
        assertThat(api.get(PUBLIC + "/" + id).status()).isEqualTo(404);
        assertThat(api.delete(adminPath(mosqueId, "announcements/" + id), editor.token()).status()).isEqualTo(204);

        ApiResponse invalid = api.post(adminPath(mosqueId, "announcements"), Map.of("title", "Zonder tekst"),
                editor.token());
        assertThat(invalid.status()).isEqualTo(400);
        assertThat(invalid.body().get("fieldErrors").toString()).contains("body");
    }

    private String create(String title, boolean featured, Instant expiresAt) {
        Map<String, Object> body = new HashMap<>();
        body.put("title", title);
        body.put("body", "Tekst van " + title);
        body.put("featured", featured);
        if (expiresAt != null) {
            body.put("expiresAt", expiresAt.toString());
        }
        ApiResponse created = api.post(adminPath(mosqueId, "announcements"), body, editor.token());
        assertThat(created.status()).as(created.body().toString()).isEqualTo(201);
        return created.field("id");
    }

    private String publish(String id) {
        ApiResponse published = api.post(adminPath(mosqueId, "announcements/" + id + "/publish"), null, editor.token());
        assertThat(published.status()).as(published.body().toString()).isEqualTo(200);
        return id;
    }
}
