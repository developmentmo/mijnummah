package nl.mijnummah.event.api;

import java.time.Instant;
import java.util.List;
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

/** The agenda of all mosques together, for the central app (D-060). */
@RestController
@RequestMapping("/api/v1/public/events")
@Tag(name = "Publiek: agenda")
class PublicAgendaController {

    /** More slugs than this in one request is not a realistic filter. */
    static final int MAX_MOSQUES = 100;

    private final PublicEventService eventService;
    private final PublicEventResponses responses;

    PublicAgendaController(PublicEventService eventService, PublicEventResponses responses) {
        this.eventService = eventService;
        this.responses = responses;
    }

    @GetMapping
    @Operation(summary = "Agenda van alle actieve moskeeën, oplopend op starttijd",
            description = "Met mosque (herhaalbaar, slug) alleen die moskeeën, bijv. de gevolgde.")
    PageResponse<EventResponse> list(@AuthenticationPrincipal AuthenticatedUser viewer,
                                     @Parameter(description = "Slug van een moskee; mag vaker voorkomen")
                                     @RequestParam(name = "mosque", required = false) List<String> mosques,
                                     @RequestParam(required = false) EventCategory category,
                                     @RequestParam(required = false) Instant from,
                                     @RequestParam(required = false) Instant to,
                                     @RequestParam(required = false) Integer page,
                                     @RequestParam(required = false) Integer size) {
        var pageable = PageRequests.of(page, size, Sort.by("startsAt", "id"));
        return responses.page(eventService.listAcross(limit(mosques), viewer, category, from, to, pageable));
    }

    @GetMapping("/{eventId}")
    @Operation(summary = "Eén activiteit van een actieve moskee")
    EventResponse get(@AuthenticationPrincipal AuthenticatedUser viewer, @PathVariable UUID eventId) {
        return responses.one(eventService.get(eventId, viewer));
    }

    static List<String> limit(List<String> slugs) {
        return slugs == null ? null : slugs.stream().filter(s -> s != null && !s.isBlank()).distinct()
                .limit(MAX_MOSQUES).toList();
    }
}
