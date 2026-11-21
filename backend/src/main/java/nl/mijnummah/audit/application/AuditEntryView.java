package nl.mijnummah.audit.application;

import java.time.Instant;
import java.util.UUID;

/**
 * An audit log entry for display.
 *
 * @param actorUserId      null when the account has since been deleted
 * @param actorDisplayName null when the account has since been deleted
 */
public record AuditEntryView(UUID id, Instant occurredAt, UUID actorUserId, String actorDisplayName, UUID mosqueId,
                             String action, String targetType, UUID targetId) {
}
