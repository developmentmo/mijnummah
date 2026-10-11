package nl.mijnummah.notification.api;

import java.util.UUID;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import nl.mijnummah.common.web.PageRequests;
import nl.mijnummah.common.web.PageResponse;
import nl.mijnummah.identity.AuthenticatedUser;
import nl.mijnummah.notification.PushContentType;
import nl.mijnummah.notification.PushNotificationView;
import nl.mijnummah.notification.application.PushAudience;
import nl.mijnummah.notification.application.PushNotificationService;

@RestController
@RequestMapping("/api/v1/admin/mosques/{mosqueId}/notifications")
@Tag(name = "Beheer: pushmeldingen")
class AdminNotificationController {

    private final PushNotificationService notifications;

    AdminNotificationController(PushNotificationService notifications) {
        this.notifications = notifications;
    }

    @GetMapping
    @Operation(summary = "Verstuurde pushmeldingen, nieuwste eerst",
            description = "Met contentType en contentId: alleen de melding over dat item (hooguit één).")
    PageResponse<PushNotificationView> list(@AuthenticationPrincipal AuthenticatedUser actor,
                                            @PathVariable UUID mosqueId,
                                            @RequestParam(required = false) PushContentType contentType,
                                            @RequestParam(required = false) UUID contentId,
                                            @RequestParam(required = false) Integer page,
                                            @RequestParam(required = false) Integer size) {
        var pageable = PageRequests.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id")));
        if (contentType != null && contentId != null) {
            var found = notifications.forContent(actor, mosqueId, contentType, contentId).stream().toList();
            return PageResponse.of(new PageImpl<>(found, pageable, found.size()), view -> view);
        }
        return PageResponse.of(notifications.list(actor, mosqueId, pageable), view -> view);
    }

    @GetMapping("/audience")
    @Operation(summary = "Hoeveel volgers en apparaten een pushmelding van dit soort nu bereikt")
    PushAudience audience(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable UUID mosqueId,
                          @RequestParam PushContentType contentType) {
        return notifications.audience(actor, mosqueId, contentType);
    }
}
