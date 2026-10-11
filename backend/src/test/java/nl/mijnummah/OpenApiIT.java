package nl.mijnummah;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import nl.mijnummah.ApiTestClient.ApiResponse;

class OpenApiIT extends AbstractIntegrationTest {

    @Test
    void documentDescribesTheApiWithoutLeakingEntities() {
        ApiResponse docs = api.get("/v3/api-docs");
        assertThat(docs.status()).isEqualTo(200);
        assertThat(docs.body().get("info").get("title").asString()).isEqualTo("Mijn Ummah API");
        var paths = docs.body().get("paths");
        assertThat(paths.has("/api/v1/public/mosques/{slug}/events")).isTrue();
        assertThat(paths.has("/api/v1/admin/mosques/{mosqueId}/media")).isTrue();
        assertThat(paths.has("/api/v1/events/{eventId}/registrations")).isTrue();
        assertThat(paths.has("/api/v1/me/follows/{mosqueId}/notifications")).isTrue();

        String schemas = docs.body().get("components").get("schemas").toString();
        // DTOs only: entity-only fields such as password hashes and audit actors never appear in the contract
        assertThat(schemas).doesNotContain("passwordHash").doesNotContain("createdBy").doesNotContain("tokenHash");
    }
}
