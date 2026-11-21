package nl.mijnummah.audit.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import nl.mijnummah.common.audit.AuditEvent;

/** One entry of the audit log. Never changed after it is written. */
@Entity
@Table(name = "audit_log")
public class AuditLogEntry {

    @Id
    private UUID id;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "actor_user_id", updatable = false)
    private UUID actorUserId;

    @Column(name = "mosque_id", updatable = false)
    private UUID mosqueId;

    @Column(name = "action", nullable = false, updatable = false, length = 60)
    private String action;

    @Column(name = "target_type", nullable = false, updatable = false, length = 30)
    private String targetType;

    @Column(name = "target_id", updatable = false)
    private UUID targetId;

    protected AuditLogEntry() {
        // for JPA
    }

    public static AuditLogEntry of(AuditEvent event, Instant now) {
        AuditLogEntry entry = new AuditLogEntry();
        entry.id = UUID.randomUUID();
        entry.occurredAt = now;
        entry.actorUserId = event.actorUserId();
        entry.mosqueId = event.mosqueId();
        entry.action = event.action();
        entry.targetType = event.targetType();
        entry.targetId = event.targetId();
        return entry;
    }

    public UUID getId() {
        return id;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    /** Null when the account has since been deleted. */
    public UUID getActorUserId() {
        return actorUserId;
    }

    public UUID getMosqueId() {
        return mosqueId;
    }

    public String getAction() {
        return action;
    }

    public String getTargetType() {
        return targetType;
    }

    public UUID getTargetId() {
        return targetId;
    }
}
