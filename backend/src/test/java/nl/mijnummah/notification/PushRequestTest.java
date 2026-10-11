package nl.mijnummah.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class PushRequestTest {

    @Test
    void longTextIsShortenedWithAnEllipsis() {
        PushRequest request = new PushRequest(UUID.randomUUID(), PushContentType.ANNOUNCEMENT, UUID.randomUUID(),
                "  " + "t".repeat(150) + " ", "b".repeat(300));
        assertThat(request.title()).hasSize(100).endsWith("…");
        assertThat(request.body()).hasSize(250).endsWith("…");
    }

    @Test
    void shortTextIsKeptAndEmojiAreNotCutInHalf() {
        assertThat(PushRequest.shorten("Iftar", 100)).isEqualTo("Iftar");
        String emoji = "🌙".repeat(10);
        assertThat(PushRequest.shorten(emoji, 5)).isEqualTo("🌙".repeat(4) + "…");
    }

    @Test
    void contentTypesMapToTheirNotificationPreference() {
        assertThat(PushContentType.ANNOUNCEMENT.topic().name()).isEqualTo("ANNOUNCEMENTS");
        assertThat(PushContentType.EVENT.topic().name()).isEqualTo("EVENTS");
        assertThat(PushContentType.MEDIA.topic().name()).isEqualTo("MEDIA");
    }
}
