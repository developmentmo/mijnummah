package nl.mijnummah.notification.api;

import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import nl.mijnummah.common.web.PageRequests;
import nl.mijnummah.common.web.PageResponse;
import nl.mijnummah.identity.AuthenticatedUser;
import nl.mijnummah.notification.application.NotificationInboxService;
import nl.mijnummah.notification.application.NotificationInboxService.InboxItem;

@RestController
@RequestMapping("/api/v1/me/notifications")
@Tag(name = "Pushmeldingen: overzicht in de app")
class InboxController {

    private final NotificationInboxService inbox;

    InboxController(NotificationInboxService inbox) {
        this.inbox = inbox;
    }

    @GetMapping
    @Operation(summary = "Meldingen van de moskeeën die ik volg, van de laatste 30 dagen, nieuwste eerst")
    PageResponse<InboxItem> mine(@AuthenticationPrincipal AuthenticatedUser actor,
                                 @RequestParam(required = false) Integer page,
                                 @RequestParam(required = false) Integer size) {
        return PageResponse.of(inbox.mine(actor, PageRequests.of(page, size, Sort.unsorted())), item -> item);
    }
}
