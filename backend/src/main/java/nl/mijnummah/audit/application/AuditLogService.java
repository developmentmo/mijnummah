package nl.mijnummah.audit.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import nl.mijnummah.audit.domain.AuditLogEntry;
import nl.mijnummah.audit.infrastructure.AuditLogRepository;
import nl.mijnummah.common.audit.AuditEvent;
import nl.mijnummah.identity.AuthenticatedUser;
import nl.mijnummah.identity.UserDirectory;
import nl.mijnummah.identity.UserSummary;
import nl.mijnummah.mosque.MosqueAccessDeniedException;
import nl.mijnummah.mosque.MosqueAccessService;
import nl.mijnummah.mosque.MosquePermission;

@Service
public class AuditLogService {

    private final AuditLogRepository entries;
    private final MosqueAccessService access;
    private final UserDirectory users;
    private final Clock clock;

    public AuditLogService(AuditLogRepository entries, MosqueAccessService access, UserDirectory users,
                           Clock clock) {
        this.entries = entries;
        this.access = access;
        this.users = users;
        this.clock = clock;
    }

    /**
     * Stores an audit event in the transaction of the action that published it (MANDATORY: an action outside a
     * transaction is a programming error, not something to log silently on its own).
     */
    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(AuditEvent event) {
        entries.save(AuditLogEntry.of(event, Instant.now(clock)));
    }

    /**
     * Newest first. Without a mosque: the whole platform, for platform administrators only. With a mosque: its
     * entries, for platform administrators and administrators of that mosque (not editors).
     */
    @Transactional(readOnly = true)
    public Page<AuditEntryView> list(AuthenticatedUser actor, UUID mosqueId, Pageable pageable) {
        Page<AuditLogEntry> page;
        if (mosqueId == null) {
            if (!actor.isPlatformAdmin()) {
                throw new MosqueAccessDeniedException();
            }
            page = entries.findAll(pageable);
        } else {
            access.require(actor, mosqueId, MosquePermission.MANAGE_MOSQUE);
            page = entries.findByMosqueId(mosqueId, pageable);
        }
        Map<UUID, UserSummary> actors = users.findByIds(page.getContent().stream()
                .map(AuditLogEntry::getActorUserId).filter(Objects::nonNull).distinct().toList());
        return page.map(entry -> {
            UserSummary user = entry.getActorUserId() == null ? null : actors.get(entry.getActorUserId());
            return new AuditEntryView(entry.getId(), entry.getOccurredAt(), entry.getActorUserId(),
                    user == null ? null : user.displayName(), entry.getMosqueId(), entry.getAction(),
                    entry.getTargetType(), entry.getTargetId());
        });
    }
}
