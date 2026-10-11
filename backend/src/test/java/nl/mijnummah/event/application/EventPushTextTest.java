package nl.mijnummah.event.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import nl.mijnummah.event.domain.EventCategory;
import nl.mijnummah.event.domain.EventDetails;
import nl.mijnummah.event.domain.Visibility;

class EventPushTextTest {

    private static final ZoneId AMSTERDAM = ZoneId.of("Europe/Amsterdam");

    @Test
    void dateAndTimeAreInTheMosquesTimeZoneWithoutDots() {
        // 17:30 UTC on Saturday 14 November 2026 is 18:30 in Amsterdam (winter time)
        assertThat(EventAdminService.defaultPushBody(details(Instant.parse("2026-11-14T17:30:00Z"), "Grote zaal"),
                AMSTERDAM)).isEqualTo("za 14 nov, 18:30 · Grote zaal");
        // summer time
        assertThat(EventAdminService.defaultPushBody(details(Instant.parse("2026-06-05T17:30:00Z"), null), AMSTERDAM))
                .isEqualTo("vr 5 jun, 19:30");
    }

    private static EventDetails details(Instant startsAt, String location) {
        return new EventDetails("Lezing", null, EventCategory.LECTURE, startsAt, startsAt.plusSeconds(3600), location,
                null, Visibility.PUBLIC, false, null);
    }
}
