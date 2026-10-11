package nl.mijnummah.follow;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nl.mijnummah.AbstractIntegrationTest;
import nl.mijnummah.ApiTestClient.ApiResponse;

class FollowFlowIT extends AbstractIntegrationTest {

    private UUID mosqueId;
    private Account visitor;

    @BeforeEach
    void mosqueAndVisitor() {
        mosqueId = mosque("noeroel-islam");
        visitor = user("bezoeker");
    }

    @Test
    void followingNeedsAnAccount() {
        assertThat(api.put("/api/v1/me/follows/" + mosqueId, null, null).status()).isEqualTo(401);
        assertThat(api.get("/api/v1/me/follows").status()).isEqualTo(401);
    }

    @Test
    void followIsIdempotentAndAllNotificationsAreOnByDefault() {
        ApiResponse first = api.put("/api/v1/me/follows/" + mosqueId, null, visitor.token());
        assertThat(first.status()).isEqualTo(200);
        assertThat(first.field("mosqueSlug")).isEqualTo("noeroel-islam");
        assertThat(first.body().get("notifications").toString())
                .isEqualTo("{\"ANNOUNCEMENTS\":true,\"EVENTS\":true,\"MEDIA\":true}");

        ApiResponse again = api.put("/api/v1/me/follows/" + mosqueId, null, visitor.token());
        assertThat(again.field("followedSince")).isEqualTo(first.field("followedSince"));
        assertThat(api.get("/api/v1/me/follows", visitor.token()).body().size()).isEqualTo(1);
    }

    @Test
    void preferencesChangeOnlyTheGivenTopicsAndDisappearOnUnfollow() {
        api.put("/api/v1/me/follows/" + mosqueId, null, visitor.token());
        String path = "/api/v1/me/follows/" + mosqueId + "/notifications";

        ApiResponse changed = api.put(path, Map.of("events", false), visitor.token());
        assertThat(changed.body().get("notifications").get("EVENTS").asBoolean()).isFalse();
        assertThat(changed.body().get("notifications").get("ANNOUNCEMENTS").asBoolean()).isTrue();

        ApiResponse again = api.put(path, Map.of("media", false, "events", true), visitor.token());
        assertThat(again.body().get("notifications").get("EVENTS").asBoolean()).isTrue();
        assertThat(again.body().get("notifications").get("MEDIA").asBoolean()).isFalse();

        assertThat(api.delete("/api/v1/me/follows/" + mosqueId, visitor.token()).status()).isEqualTo(204);
        assertThat(api.delete("/api/v1/me/follows/" + mosqueId, visitor.token()).status()).isEqualTo(204);
        Integer left = jdbc.queryForObject("select count(*) from notification_preference", Integer.class);
        assertThat(left).isZero();

        ApiResponse notFollowing = api.put(path, Map.of("events", false), visitor.token());
        assertThat(notFollowing.status()).isEqualTo(404);
        assertThat(notFollowing.field("code")).isEqualTo("NOT_FOLLOWING");
    }

    @Test
    void unknownOrInactiveMosquesCannotBeFollowedAndInactiveOnesAreHidden() {
        assertThat(api.put("/api/v1/me/follows/" + UUID.randomUUID(), null, visitor.token()).status()).isEqualTo(404);

        api.put("/api/v1/me/follows/" + mosqueId, null, visitor.token());
        api.put("/api/v1/admin/mosques/" + mosqueId + "/active", Map.of("active", false), platformAdmin.token());
        assertThat(api.get("/api/v1/me/follows", visitor.token()).body().size()).isZero();
        assertThat(api.put("/api/v1/me/follows/" + mosqueId, null, visitor.token()).status()).isEqualTo(404);
    }

    @Test
    void followsArePrivatePerUser() {
        api.put("/api/v1/me/follows/" + mosqueId, null, visitor.token());
        assertThat(api.get("/api/v1/me/follows", user("ander").token()).body().size()).isZero();
    }
}
