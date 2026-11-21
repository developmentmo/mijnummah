package nl.mijnummah.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import nl.mijnummah.AbstractIntegrationTest;
import nl.mijnummah.ApiTestClient.ApiResponse;

class ImageFlowIT extends AbstractIntegrationTest {

    @Test
    void anAdminUploadsALogoThatTheAppShows() {
        UUID mosqueId = mosque("al-fath");
        Account admin = mosqueAdmin(mosqueId, "admin");

        ApiResponse uploaded = api.upload(images(mosqueId), "logo.png", TestImages.png(512, 512, true), admin.token());
        assertThat(uploaded.status()).isEqualTo(201);
        String imageId = uploaded.field("id");
        assertThat(uploaded.field("path")).isEqualTo("/api/v1/public/images/" + imageId);

        ApiResponse set = api.put(images(mosqueId), Map.of("logoImageId", imageId), admin.token());
        assertThat(set.status()).isEqualTo(200);
        assertThat(set.field("logoImageId")).isEqualTo(imageId);

        ApiResponse profile = api.get("/api/v1/public/mosques/al-fath");
        assertThat(profile.field("logoUrl")).isEqualTo("/api/v1/public/images/" + imageId);
        assertThat(profile.field("coverUrl")).isNull();

        HttpResponse<byte[]> image = api.getBytes(profile.field("logoUrl"));
        assertThat(image.statusCode()).isEqualTo(200);
        assertThat(image.headers().firstValue("Content-Type")).hasValue("image/png");
        assertThat(image.headers().firstValue("Cache-Control").orElseThrow()).contains("max-age=31536000");
        assertThat(image.body()).isNotEmpty();

        Map<String, Object> remove = new HashMap<>();
        remove.put("logoImageId", null);
        assertThat(api.put(images(mosqueId), remove, admin.token()).field("logoImageId")).isNull();
    }

    @Test
    void aMosqueCannotUseAnImageOfAnotherMosque() {
        UUID mosqueA = mosque("moskee-a");
        UUID mosqueB = mosque("moskee-b");
        Account adminA = mosqueAdmin(mosqueA, "admin-a");
        Account adminB = mosqueAdmin(mosqueB, "admin-b");
        String imageOfB = api.upload(images(mosqueB), "b.jpg", TestImages.jpeg(300, 200), adminB.token()).field("id");

        ApiResponse stolen = api.put(images(mosqueA), Map.of("coverImageId", imageOfB), adminA.token());
        assertThat(stolen.status()).isEqualTo(404);
        assertThat(stolen.field("code")).isEqualTo("IMAGE_NOT_FOUND");
        assertThat(api.upload(images(mosqueB), "x.jpg", TestImages.jpeg(300, 200), adminA.token()).status())
                .isEqualTo(403);
    }

    @Test
    void anEditorUploadsButOnlyAnAdminChangesTheProfileImages() {
        UUID mosqueId = mosque("an-noor");
        Account editor = user("editor");
        grant(mosqueId, editor, "EDITOR");

        ApiResponse uploaded = api.upload(images(mosqueId), "foto.jpg", TestImages.jpeg(400, 300), editor.token());
        assertThat(uploaded.status()).isEqualTo(201);
        assertThat(api.put(images(mosqueId), Map.of("coverImageId", uploaded.field("id")), editor.token()).status())
                .isEqualTo(403);
        assertThat(api.upload(images(mosqueId), "foto.jpg", TestImages.jpeg(400, 300), null).status())
                .isEqualTo(401);
    }

    @Test
    void refusesFilesThatAreNotImages() {
        UUID mosqueId = mosque("as-salaam");
        Account admin = mosqueAdmin(mosqueId, "admin");
        ApiResponse refused = api.upload(images(mosqueId), "logo.svg",
                "<svg xmlns=\"http://www.w3.org/2000/svg\"/>".getBytes(), admin.token());
        assertThat(refused.status()).isEqualTo(400);
        assertThat(refused.field("code")).isEqualTo("INVALID_IMAGE");
        assertThat(api.getBytes("/api/v1/public/images/" + UUID.randomUUID()).statusCode()).isEqualTo(404);
    }

    private static String images(UUID mosqueId) {
        return "/api/v1/admin/mosques/" + mosqueId + "/images";
    }
}
