package nl.mijnummah.announcement.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class AnnouncementTest {

    private static final Instant NOW = Instant.parse("2026-10-11T10:00:00Z");
    private final UUID actor = UUID.randomUUID();

    @Test
    void expiresAtTheExpiryMoment() {
        Announcement announcement = Announcement.draft(UUID.randomUUID(),
                new AnnouncementDetails("Iftar", "Iedereen welkom", false, NOW.plusSeconds(60)), actor, NOW);
        assertThat(announcement.isExpired(NOW)).isFalse();
        assertThat(announcement.isExpired(NOW.plusSeconds(60))).isTrue();
    }

    @Test
    void anExpiredAnnouncementCannotBePublished() {
        Announcement announcement = Announcement.draft(UUID.randomUUID(),
                new AnnouncementDetails("Iftar", "Iedereen welkom", false, NOW.minusSeconds(1)), actor, NOW);
        assertThatThrownBy(() -> announcement.publish(actor, NOW)).isInstanceOf(InvalidAnnouncementException.class);
        assertThat(announcement.getStatus()).isEqualTo(AnnouncementStatus.DRAFT);
    }

    @Test
    void publishingKeepsTheFirstPublicationTime() {
        Announcement announcement = Announcement.draft(UUID.randomUUID(),
                new AnnouncementDetails("Iftar", "Iedereen welkom", true, null), actor, NOW);
        announcement.publish(actor, NOW);
        announcement.unpublish(actor, NOW.plusSeconds(5));
        announcement.publish(actor, NOW.plusSeconds(10));
        assertThat(announcement.getPublishedAt()).isEqualTo(NOW);
    }
}
