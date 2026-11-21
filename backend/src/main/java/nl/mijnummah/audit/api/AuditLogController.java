package nl.mijnummah.audit.api;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import nl.mijnummah.audit.application.AuditEntryView;
import nl.mijnummah.audit.application.AuditLogService;
import nl.mijnummah.common.web.PageRequests;
import nl.mijnummah.common.web.PageResponse;
import nl.mijnummah.identity.AuthenticatedUser;

@RestController
@RequestMapping("/api/v1/admin/audit-log")
@Tag(name = "Beheer: auditlog")
class AuditLogController {

    private final AuditLogService auditLog;

    AuditLogController(AuditLogService auditLog) {
        this.auditLog = auditLog;
    }

    @GetMapping
    @Operation(summary = "Beheeracties, nieuwste eerst (zonder moskee: alleen platformbeheerder; met moskee: ook "
            + "beheerder van die moskee)")
    PageResponse<AuditEntryResponse> list(@AuthenticationPrincipal AuthenticatedUser actor,
                                          @RequestParam(required = false) UUID mosqueId,
                                          @RequestParam(required = false) Integer page,
                                          @RequestParam(required = false) Integer size) {
        var pageable = PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "occurredAt"));
        return PageResponse.of(auditLog.list(actor, mosqueId, pageable), AuditEntryResponse::from);
    }

    record AuditEntryResponse(UUID id, Instant occurredAt, Actor actor, UUID mosqueId, String mosqueName,
                              String action, String targetType, UUID targetId) {

        /** Null when the account has since been deleted. */
        record Actor(UUID id, String displayName) {
        }

        static AuditEntryResponse from(AuditEntryView view) {
            Actor actor = view.actorUserId() == null ? null : new Actor(view.actorUserId(), view.actorDisplayName());
            return new AuditEntryResponse(view.id(), view.occurredAt(), actor, view.mosqueId(), view.mosqueName(),
                    view.action(), view.targetType(), view.targetId());
        }
    }
}
