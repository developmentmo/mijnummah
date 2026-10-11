package nl.mijnummah.media;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nl.mijnummah.AbstractIntegrationTest;
import nl.mijnummah.ApiTestClient.ApiResponse;

class MediaFlowIT extends AbstractIntegrationTest {

    private static final String PUBLIC = "/api/v1/public/mosques/noeroel-islam/media";

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
    void aYouTubeLinkBecomesAPlayableReferenceAfterPublishing() {
        ApiResponse created = api.post(adminPath(mosqueId, "media"),
                media("Tafsir Al-Fatiha", "https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=10s", "Imam Yusuf", "LECTURE"),
                editor.token());
        assertThat(created.status()).isEqualTo(201);
        assertThat(created.field("status")).isEqualTo("DRAFT");
        String id = created.field("id");
        assertThat(api.get(PUBLIC + "/" + id).status()).isEqualTo(404);

        api.post(adminPath(mosqueId, "media/" + id + "/publish"), null, editor.token());
        ApiResponse detail = api.get(PUBLIC + "/" + id);
        assertThat(detail.status()).isEqualTo(200);
        var video = detail.body().get("video");
        assertThat(video.get("provider").asString()).isEqualTo("YOUTUBE");
        assertThat(video.get("videoId").asString()).isEqualTo("dQw4w9WgXcQ");
        assertThat(video.get("embedUrl").asString()).isEqualTo("https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ");
        assertThat(detail.field("thumbnailUrl")).isEqualTo("https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg");
    }

    @Test
    void unsupportedLinksAndDuplicatesAreRejected() {
        ApiResponse vimeo = api.post(adminPath(mosqueId, "media"),
                media("Elders", "https://vimeo.com/123", null, "LECTURE"), editor.token());
        assertThat(vimeo.status()).isEqualTo(400);
        assertThat(vimeo.field("code")).isEqualTo("INVALID_VIDEO_URL");

        api.post(adminPath(mosqueId, "media"), media("Eerste", "dQw4w9WgXcQ", null, "LECTURE"), editor.token());
        ApiResponse duplicate = api.post(adminPath(mosqueId, "media"),
                media("Tweede", "https://youtu.be/dQw4w9WgXcQ", null, "LECTURE"), editor.token());
        assertThat(duplicate.status()).isEqualTo(409);
        assertThat(duplicate.field("code")).isEqualTo("VIDEO_ALREADY_ADDED");
    }

    @Test
    void searchFindsTitleAndSpeakerAndCategoryFilters() {
        published(media("Tafsir Al-Baqara", "aaaaaaaaaaa", "Imam Yusuf", "LECTURE"));
        published(media("Vrijdagpreek over geduld", "bbbbbbbbbbb", "Imam Ali", "KHUTBAH"));
        published(media("Koranrecitatie", "ccccccccccc", "Qari Hamza", "QURAN"));

        assertThat(total(PUBLIC)).isEqualTo(3);
        assertThat(total(PUBLIC + "?q=tafsir")).isEqualTo(1);
        assertThat(total(PUBLIC + "?q=IMAM")).isEqualTo(2);
        assertThat(total(PUBLIC + "?q=100%25")).isZero();
        assertThat(total(PUBLIC + "?category=KHUTBAH")).isEqualTo(1);
        assertThat(total(PUBLIC + "?q=imam&category=KHUTBAH")).isEqualTo(1);
    }

    @Test
    void relatedItemsShareCategoryOrSpeaker() {
        String main = published(media("Tafsir deel 1", "aaaaaaaaaaa", "Imam Yusuf", "LECTURE"));
        published(media("Tafsir deel 2", "bbbbbbbbbbb", "Imam Yusuf", "LECTURE"));
        published(media("Preek", "ccccccccccc", "Imam Yusuf", "KHUTBAH"));
        published(media("Recitatie", "ddddddddddd", "Qari Hamza", "QURAN"));
        ApiResponse related = api.get(PUBLIC + "/" + main + "/related");
        assertThat(related.status()).isEqualTo(200);
        assertThat(related.body().size()).isEqualTo(2);
        assertThat(related.body().toString()).doesNotContain(main).doesNotContain("Recitatie");
    }

    @Test
    void registeredUsersOnlyMediaIsHiddenFromAnonymousVisitors() {
        Map<String, Object> body = media("Besloten les", "aaaaaaaaaaa", null, "COURSE");
        body.put("visibility", "REGISTERED_USERS");
        String id = published(body);
        assertThat(api.get(PUBLIC + "/" + id).status()).isEqualTo(404);
        assertThat(api.get(PUBLIC + "/" + id, user("lid").token()).status()).isEqualTo(200);
    }

    @Test
    void mediaCanLinkToAnEventOfTheSameMosque() {
        UUID event = publishedEvent(mosqueId, editor.token());
        Map<String, Object> body = media("Opname lezing", "aaaaaaaaaaa", null, "LECTURE");
        body.put("relatedEventId", event.toString());
        String id = published(body);
        assertThat(api.get(PUBLIC + "/" + id).body().get("relatedEvent").get("id").asString())
                .isEqualTo(event.toString());
    }

    @Test
    void unpublishAndDeleteRemoveItFromTheLibrary() {
        String id = published(media("Tijdelijk", "aaaaaaaaaaa", null, "OTHER"));
        api.post(adminPath(mosqueId, "media/" + id + "/unpublish"), null, editor.token());
        assertThat(api.get(PUBLIC + "/" + id).status()).isEqualTo(404);
        assertThat(api.delete(adminPath(mosqueId, "media/" + id), editor.token()).status()).isEqualTo(204);
        assertThat(api.get(adminPath(mosqueId, "media/" + id), editor.token()).status()).isEqualTo(404);
    }

    private String published(Map<String, Object> body) {
        ApiResponse created = api.post(adminPath(mosqueId, "media"), body, editor.token());
        assertThat(created.status()).as(created.body().toString()).isEqualTo(201);
        api.post(adminPath(mosqueId, "media/" + created.field("id") + "/publish"), null, editor.token());
        return created.field("id");
    }

    private long total(String path) {
        return api.get(path).body().get("totalItems").asLong();
    }

    private static Map<String, Object> media(String title, String videoUrl, String speaker, String category) {
        Map<String, Object> body = new HashMap<>();
        body.put("title", title);
        body.put("videoUrl", videoUrl);
        body.put("category", category);
        if (speaker != null) {
            body.put("speaker", speaker);
        }
        return body;
    }
}
