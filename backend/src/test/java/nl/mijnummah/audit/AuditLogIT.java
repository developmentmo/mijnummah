package nl.mijnummah.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import tools.jackson.databind.JsonNode;

import nl.mijnummah.AbstractIntegrationTest;
import nl.mijnummah.ApiTestClient.ApiResponse;
import nl.mijnummah.audit.application.AuditRetention;

class AuditLogIT extends AbstractIntegrationTest {

    @Autowired
    private AuditRetention retention;

    @Test
    void managementActionsAreLoggedWithActorAndMosque() {
        UUID mosqueId = mosque("al-fath");
        Account admin = mosqueAdmin(mosqueId, "admin");
        UUID event = publishedEvent(mosqueId, admin.token());

        ApiResponse log = api.get("/api/v1/admin/audit-log?mosqueId=" + mosqueId, admin.token());
        assertThat(log.status()).isEqualTo(200);
        JsonNode items = log.body().get("items");
        assertThat(items.findValuesAsString("action")).as("newest first")
                .containsExactly("EVENT_PUBLISHED", "EVENT_CREATED", "ADMINISTRATOR_ADDED", "MOSQUE_CREATED");
        JsonNode published = items.get(0);
        assertThat(published.get("targetType").asString()).isEqualTo("EVENT");
        assertThat(published.get("targetId").asString()).isEqualTo(event.toString());
        assertThat(published.get("actor").get("id").asString()).isEqualTo(admin.id().toString());
        assertThat(published.get("mosqueId").asString()).isEqualTo(mosqueId.toString());
    }

    @Test
    void onlyAdministratorsOfTheMosqueOrThePlatformAdminReadTheLog() {
        UUID mosqueA = mosque("moskee-a");
        UUID mosqueB = mosque("moskee-b");
        Account adminA = mosqueAdmin(mosqueA, "admin-a");
        Account editorA = user("editor-a");
        grant(mosqueA, editorA, "EDITOR");

        assertThat(api.get("/api/v1/admin/audit-log?mosqueId=" + mosqueB, adminA.token()).status()).isEqualTo(403);
        assertThat(api.get("/api/v1/admin/audit-log?mosqueId=" + mosqueA, editorA.token()).status()).isEqualTo(403);
        assertThat(api.get("/api/v1/admin/audit-log", adminA.token()).status())
                .as("the whole platform is for the platform admin only").isEqualTo(403);
        assertThat(api.get("/api/v1/admin/audit-log").status()).isEqualTo(401);

        ApiResponse all = api.get("/api/v1/admin/audit-log", platformAdmin.token());
        assertThat(all.status()).isEqualTo(200);
        assertThat(all.body().get("items").findValuesAsString("mosqueId"))
                .contains(mosqueA.toString(), mosqueB.toString());
    }

    @Test
    void aFailedActionLeavesNoEntry() {
        UUID mosqueId = mosque("an-noor");
        Account admin = mosqueAdmin(mosqueId, "admin");
        UUID event = publishedEvent(mosqueId, admin.token());
        int before = count();

        assertThat(api.delete(adminPath(mosqueId, "events/" + event), admin.token()).status())
                .as("a published event cannot be deleted").isEqualTo(422);
        assertThat(count()).isEqualTo(before);
    }

    @Test
    void platformActionsAndStatusChangesAreLogged() {
        UUID mosqueId = mosque("as-sunnah");
        Account someone = user("someone");
        api.post("/api/v1/admin/users/" + someone.id() + "/block", null, platformAdmin.token());
        api.patch("/api/v1/admin/mosques/" + mosqueId + "/status", Map.of("status", "BLOCKED"),
                platformAdmin.token());

        assertThat(api.get("/api/v1/admin/audit-log", platformAdmin.token()).body().get("items")
                .findValuesAsString("action")).contains("ACCOUNT_BLOCKED", "MOSQUE_STATUS_BLOCKED");
    }

    @Test
    void oldEntriesAreRemoved() {
        mosque("al-huda");
        assertThat(count()).isPositive();
        retention.purge(Instant.now().plus(731, ChronoUnit.DAYS));
        assertThat(count()).isZero();
    }

    private int count() {
        return jdbc.queryForObject("select count(*) from audit_log", Integer.class);
    }
}
