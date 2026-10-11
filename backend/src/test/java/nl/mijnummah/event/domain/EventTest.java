package nl.mijnummah.event.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class EventTest {

    private static final Instant NOW = Instant.parse("2026-10-11T10:00:00Z");
    private final UUID mosqueId = UUID.randomUUID();
    private final UUID actor = UUID.randomUUID();

    @Test
    void newEventIsADraftAndPublishingKeepsTheFirstPublicationTime() {
        Event event = Event.draft(mosqueId, details(), actor, NOW);
        assertThat(event.getStatus()).isEqualTo(EventStatus.DRAFT);
        assertThat(event.isPubliclyListed()).isFalse();

        event.publish(actor, NOW.plusSeconds(60));
        event.unpublish(actor, NOW.plusSeconds(120));
        event.publish(actor, NOW.plusSeconds(180));

        assertThat(event.getStatus()).isEqualTo(EventStatus.PUBLISHED);
        assertThat(event.getPublishedAt()).isEqualTo(NOW.plusSeconds(60));
    }

    @Test
    void onlyAPublishedEventCanBeCancelledAndCancellationIsFinal() {
        Event event = Event.draft(mosqueId, details(), actor, NOW);
        assertThatThrownBy(() -> event.cancel(actor, NOW))
                .isInstanceOf(EventRuleViolationException.class).extracting("code").isEqualTo("EVENT_NOT_PUBLISHED");

        event.publish(actor, NOW);
        event.cancel(actor, NOW);
        assertThat(event.getStatus()).isEqualTo(EventStatus.CANCELLED);
        assertThat(event.isPubliclyListed()).isTrue();

        assertThatThrownBy(() -> event.publish(actor, NOW)).extracting("code").isEqualTo("EVENT_CANCELLED");
        assertThatThrownBy(() -> event.update(details(), actor, NOW)).extracting("code").isEqualTo("EVENT_CANCELLED");
    }

    @Test
    void detailsRejectAnEndBeforeTheStartAndCapacityWithoutRegistration() {
        Instant start = NOW.plus(Duration.ofDays(1));
        assertThatThrownBy(() -> new EventDetails("Les", null, EventCategory.CLASS, start, start, null, null, null,
                false, null)).isInstanceOf(InvalidEventException.class);
        assertThatThrownBy(() -> new EventDetails("Les", null, EventCategory.CLASS, start, null, null, null, null,
                false, 10)).isInstanceOf(InvalidEventException.class);
        assertThatThrownBy(() -> new EventDetails("Les", null, EventCategory.CLASS, start, null, null, null, null,
                true, 0)).isInstanceOf(InvalidEventException.class);
    }

    @Test
    void detailsTrimTextAndDefaultToPublicVisibility() {
        var details = new EventDetails("  Les  ", "  ", EventCategory.CLASS, NOW, null, " Zaal 1 ", null, null, false,
                null);
        assertThat(details.title()).isEqualTo("Les");
        assertThat(details.description()).isNull();
        assertThat(details.location()).isEqualTo("Zaal 1");
        assertThat(details.visibility()).isEqualTo(Visibility.PUBLIC);
    }

    @Test
    void anEventIsOverAfterItsEndOrWithoutAnEndAfterItsStart() {
        Instant start = NOW.plus(Duration.ofDays(3));
        Event withoutEnd = Event.draft(mosqueId, details(), actor, NOW);
        assertThat(withoutEnd.isOver(start.minusSeconds(1))).isFalse();
        assertThat(withoutEnd.isOver(start)).isTrue();

        Event withEnd = Event.draft(mosqueId, new EventDetails("Les", null, EventCategory.CLASS, start,
                start.plus(Duration.ofHours(2)), null, null, Visibility.PUBLIC, false, null), actor, NOW);
        assertThat(withEnd.isOver(start.plus(Duration.ofHours(1)))).isFalse();
        assertThat(withEnd.isOver(start.plus(Duration.ofHours(2)))).isTrue();
    }

    private EventDetails details() {
        return new EventDetails("Lezing", null, EventCategory.LECTURE, NOW.plus(Duration.ofDays(3)), null, null, null,
                Visibility.PUBLIC, true, 50);
    }
}
