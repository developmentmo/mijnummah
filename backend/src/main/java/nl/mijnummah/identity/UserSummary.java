package nl.mijnummah.identity;

import java.util.UUID;

/** Minimal, non-sensitive view of a user for other modules (e.g. listing a mosque's administrators). */
public record UserSummary(UUID id, String email, String displayName) {
}
