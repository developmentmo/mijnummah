package nl.mijnummah.mosque;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import nl.mijnummah.AbstractIntegrationTest;
import nl.mijnummah.ApiTestClient.ApiResponse;

class MosqueAdminIT extends AbstractIntegrationTest {

    @Test
    void platformAdminCreatesAMosqueThatIsPublicByItsSlug() {
        UUID id = mosque("noeroel-islam");
        ApiResponse profile = api.get("/api/v1/public/mosques/noeroel-islam");
        assertThat(profile.status()).isEqualTo(200);
        assertThat(profile.field("id")).isEqualTo(id.toString());
        assertThat(profile.body().get("address").get("city").asString()).isEqualTo("Utrecht");
        assertThat(profile.field("timeZone")).isEqualTo("Europe/Amsterdam");
        assertThat(api.get("/api/v1/public/mosques").body().size()).isEqualTo(1);
        assertThat(api.get("/api/v1/public/mosques/unknown").field("code")).isEqualTo("MOSQUE_NOT_FOUND");
    }

    @Test
    void slugsAreUniqueAndValidated() {
        mosque("al-fath");
        ApiResponse duplicate = api.post("/api/v1/admin/mosques",
                Map.of("slug", "al-fath", "details", Map.of("name", "Dubbel")), platformAdmin.token());
        assertThat(duplicate.status()).isEqualTo(409);
        assertThat(duplicate.field("code")).isEqualTo("SLUG_ALREADY_TAKEN");

        ApiResponse invalid = api.post("/api/v1/admin/mosques",
                Map.of("slug", "Geen Slug!", "details", Map.of("name", "")), platformAdmin.token());
        assertThat(invalid.status()).isEqualTo(400);
        assertThat(invalid.field("code")).isEqualTo("VALIDATION_FAILED");
        assertThat(invalid.body().get("fieldErrors").toString()).contains("slug").contains("details.name");
    }

    @Test
    void onlyAPlatformAdminCreatesOrDeactivatesMosques() {
        UUID id = mosque("as-salaam");
        Account admin = mosqueAdmin(id, "admin");
        assertThat(api.post("/api/v1/admin/mosques", Map.of("slug", "nieuw", "details", Map.of("name", "Nieuw")),
                admin.token()).status()).isEqualTo(403);
        assertThat(api.put("/api/v1/admin/mosques/" + id + "/active", Map.of("active", false), admin.token()).status())
                .isEqualTo(403);

        ApiResponse deactivated = api.put("/api/v1/admin/mosques/" + id + "/active", Map.of("active", false),
                platformAdmin.token());
        assertThat(deactivated.status()).isEqualTo(200);
        assertThat(api.get("/api/v1/public/mosques/as-salaam").status()).isEqualTo(404);
        assertThat(api.get("/api/v1/public/mosques/as-salaam/events").status()).isEqualTo(404);
    }

    @Test
    void adminEditsTheMosqueAndEditorOnlyReadsIt() {
        UUID id = mosque("mimar-sinan");
        Account admin = mosqueAdmin(id, "admin");
        Account editor = user("editor");
        grant(id, editor, "EDITOR");

        Map<String, Object> details = Map.of("name", "Mimar Sinan Moskee", "street", "Kerkstraat 1",
                "postalCode", "3511 AB", "city", "Utrecht", "email", "info@example.nl",
                "websiteUrl", "https://example.nl", "timeZone", "Europe/Amsterdam");
        ApiResponse updated = api.put("/api/v1/admin/mosques/" + id, details, admin.token());
        assertThat(updated.status()).isEqualTo(200);
        assertThat(updated.field("name")).isEqualTo("Mimar Sinan Moskee");
        assertThat(updated.field("myRole")).isEqualTo("ADMIN");

        assertThat(api.get("/api/v1/admin/mosques/" + id, editor.token()).field("myRole")).isEqualTo("EDITOR");
        ApiResponse editorUpdate = api.put("/api/v1/admin/mosques/" + id, details, editor.token());
        assertThat(editorUpdate.status()).isEqualTo(403);
        assertThat(editorUpdate.field("code")).isEqualTo("MOSQUE_ACCESS_DENIED");

        ApiResponse badZone = api.put("/api/v1/admin/mosques/" + id, Map.of("name", "X", "timeZone", "Mars/Olympus"),
                admin.token());
        assertThat(badZone.status()).isEqualTo(400);
        assertThat(badZone.field("code")).isEqualTo("INVALID_MOSQUE");
    }

    @Test
    void adminManagesAdministratorsButAMosqueKeepsAtLeastOneAdmin() {
        UUID id = mosque("ayasofya");
        Account admin = mosqueAdmin(id, "admin");
        Account editor = user("editor");

        ApiResponse added = api.post("/api/v1/admin/mosques/" + id + "/administrators",
                Map.of("email", editor.email(), "role", "EDITOR"), admin.token());
        assertThat(added.status()).isEqualTo(201);
        assertThat(api.post("/api/v1/admin/mosques/" + id + "/administrators",
                Map.of("email", editor.email(), "role", "ADMIN"), admin.token()).field("code"))
                .isEqualTo("ADMINISTRATOR_ALREADY_EXISTS");
        assertThat(api.post("/api/v1/admin/mosques/" + id + "/administrators",
                Map.of("email", "nobody@example.com", "role", "ADMIN"), admin.token()).field("code"))
                .isEqualTo("USER_NOT_FOUND");

        // editors cannot manage administrators
        assertThat(api.get("/api/v1/admin/mosques/" + id + "/administrators", editor.token()).status()).isEqualTo(403);

        ApiResponse list = api.get("/api/v1/admin/mosques/" + id + "/administrators", admin.token());
        assertThat(list.body().size()).isEqualTo(2);
        String adminRowId = null;
        for (var row : list.body()) {
            if (row.get("email").asString().equals(admin.email())) {
                adminRowId = row.get("id").asString();
            }
        }
        ApiResponse removeLast = api.delete("/api/v1/admin/mosques/" + id + "/administrators/" + adminRowId,
                admin.token());
        assertThat(removeLast.status()).isEqualTo(422);
        assertThat(removeLast.field("code")).isEqualTo("LAST_ADMIN");

        assertThat(api.delete("/api/v1/admin/mosques/" + id + "/administrators/" + added.field("id"), admin.token())
                .status()).isEqualTo(204);
        assertThat(api.get("/api/v1/admin/mosques/" + id, editor.token()).status()).isEqualTo(403);
    }

    @Test
    void administeredMosquesListsOnlyMyMosques() {
        UUID a = mosque("moskee-a");
        mosque("moskee-b");
        Account admin = mosqueAdmin(a, "admin");
        ApiResponse mine = api.get("/api/v1/admin/mosques", admin.token());
        assertThat(mine.body().size()).isEqualTo(1);
        assertThat(mine.body().get(0).get("slug").asString()).isEqualTo("moskee-a");
        assertThat(api.get("/api/v1/admin/mosques", platformAdmin.token()).body().size()).isEqualTo(2);
        assertThat(api.get("/api/v1/admin/mosques", user("niemand").token()).body().size()).isZero();
    }
}
