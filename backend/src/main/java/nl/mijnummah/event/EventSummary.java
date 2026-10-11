package nl.mijnummah.event;

import java.time.Instant;
import java.util.UUID;

public record EventSummary(UUID id, String title, Instant startsAt) {
}
