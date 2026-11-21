package nl.mijnummah.common.audit;

import java.util.Objects;
import java.util.UUID;

/**
 * An important management action, published by the module that performed it with
 * {@link org.springframework.context.ApplicationEventPublisher}. The audit module stores it in the same transaction,
 * so an action that rolls back leaves no entry.
 * <p>
 * Carries no personal data beyond the actor's account ID: the target is referred to by type and ID only.
 *
 * @param actorUserId account that performed the action
 * @param mosqueId    mosque the action concerns; null for platform-wide actions (for example blocking an account)
 * @param action      what happened, upper snake case, for example {@code EVENT_PUBLISHED}
 * @param targetType  kind of object, for example {@code EVENT}
 * @param targetId    the object; null when there is none
 */
public record AuditEvent(UUID actorUserId, UUID mosqueId, String action, String targetType, UUID targetId) {

    public AuditEvent {
        Objects.requireNonNull(actorUserId, "actorUserId");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(targetType, "targetType");
        if (!action.matches("[A-Z]+(_[A-Z]+)*")) {
            throw new IllegalArgumentException("action must be upper snake case: " + action);
        }
    }

    public static AuditEvent of(UUID actorUserId, UUID mosqueId, String action, String targetType, UUID targetId) {
        return new AuditEvent(actorUserId, mosqueId, action, targetType, targetId);
    }
}
