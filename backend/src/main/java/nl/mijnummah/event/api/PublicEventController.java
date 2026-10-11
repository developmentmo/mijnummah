package nl.mijnummah.event.api;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import nl.mijnummah.common.web.PageRequests;
import nl.mijnummah.common.web.PageResponse;
import nl.mijnummah.event.application.PublicEventService;
import nl.mijnummah.event.domain.EventCategory;
import nl.mijnummah.identity.AuthenticatedUser;

@RestController
@RequestMapping("/api/v1/public/mosques/{slug}/events")
@Tag(name = "Publiek: agenda")
class PublicEventController {

    private final PublicEventService eventService;

    PublicEventController(PublicEventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    @Operation(summary = "Agenda van de moskee, oplopend op starttijd",
            description = "Standaard alleen activiteiten die nog niet voorbij zijn.")
    PageResponse<EventResponse> list(@AuthenticationPrincipal AuthenticatedUser viewer,
                                     @PathVariable String slug,
                                     @RequestParam(required = false) EventCategory category,
                                     @Parameter(description = "Alleen activiteiten die op dit moment nog niet voorbij zijn (standaard: nu)")
                                     @RequestParam(required = false) Instant from,
                                     @Parameter(description = "Alleen activiteiten die vóór dit moment beginnen")
                                     @RequestParam(required = false) Instant to,
                                     @RequestParam(required = false) Integer page,
                                     @RequestParam(required = false) Integer size) {
        var pageable = PageRequests.of(page, size, Sort.by("startsAt", "id"));
        return PageResponse.of(eventService.list(slug, viewer, category, from, to, pageable), EventResponse::from);
    }

    @GetMapping("/{eventId}")
    @Operation(summary = "Eén activiteit")
    EventResponse get(@AuthenticationPrincipal AuthenticatedUser viewer, @PathVariable String slug,
                      @PathVariable UUID eventId) {
        return EventResponse.from(eventService.get(slug, eventId, viewer));
    }
}
