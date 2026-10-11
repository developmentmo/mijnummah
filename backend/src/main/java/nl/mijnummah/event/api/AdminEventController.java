package nl.mijnummah.event.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import nl.mijnummah.common.web.PageRequests;
import nl.mijnummah.common.web.PageResponse;
import nl.mijnummah.event.application.EventAdminService;
import nl.mijnummah.event.application.RegistrantView;
import nl.mijnummah.event.application.RegistrationService;
import nl.mijnummah.event.domain.EventStatus;
import nl.mijnummah.identity.AuthenticatedUser;

@RestController
@RequestMapping("/api/v1/admin/mosques/{mosqueId}/events")
@Tag(name = "Beheer: agenda")
class AdminEventController {

    private final EventAdminService eventService;
    private final RegistrationService registrationService;

    AdminEventController(EventAdminService eventService, RegistrationService registrationService) {
        this.eventService = eventService;
        this.registrationService = registrationService;
    }

    @GetMapping
    @Operation(summary = "Alle activiteiten van de moskee (ook concepten), nieuwste starttijd eerst; "
            + "met from (bijv. komende activiteiten) eerstvolgende eerst")
    PageResponse<EventResponse.Admin> list(@AuthenticationPrincipal AuthenticatedUser actor,
                                           @PathVariable UUID mosqueId,
                                           @RequestParam(required = false) EventStatus status,
                                           @RequestParam(required = false) Instant from,
                                           @RequestParam(required = false) Integer page,
                                           @RequestParam(required = false) Integer size) {
        // An overview from a moment onwards (the portal dashboard) reads best soonest first.
        var byStart = from == null ? Sort.Order.desc("startsAt") : Sort.Order.asc("startsAt");
        var pageable = PageRequests.of(page, size, Sort.by(byStart, Sort.Order.asc("id")));
        return PageResponse.of(eventService.list(actor, mosqueId, status, from, pageable), EventResponse.Admin::from);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Activiteit aanmaken (als concept)")
    EventResponse.Admin create(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable UUID mosqueId,
                               @Valid @RequestBody EventRequest request) {
        return EventResponse.Admin.from(eventService.create(actor, mosqueId, request.toDetails()));
    }

    @GetMapping("/{eventId}")
    @Operation(summary = "Activiteit ophalen")
    EventResponse.Admin get(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable UUID mosqueId,
                            @PathVariable UUID eventId) {
        return EventResponse.Admin.from(eventService.get(actor, mosqueId, eventId));
    }

    @PutMapping("/{eventId}")
    @Operation(summary = "Activiteit wijzigen")
    EventResponse.Admin update(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable UUID mosqueId,
                               @PathVariable UUID eventId, @Valid @RequestBody EventRequest request) {
        return EventResponse.Admin.from(eventService.update(actor, mosqueId, eventId, request.toDetails()));
    }

    @PostMapping("/{eventId}/publish")
    @Operation(summary = "Publiceren")
    EventResponse.Admin publish(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable UUID mosqueId,
                                @PathVariable UUID eventId) {
        return EventResponse.Admin.from(eventService.publish(actor, mosqueId, eventId));
    }

    @PostMapping("/{eventId}/unpublish")
    @Operation(summary = "Terug naar concept (niet als er aanmeldingen zijn)")
    EventResponse.Admin unpublish(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable UUID mosqueId,
                                  @PathVariable UUID eventId) {
        return EventResponse.Admin.from(eventService.unpublish(actor, mosqueId, eventId));
    }

    @PostMapping("/{eventId}/cancel")
    @Operation(summary = "Annuleren (blijft zichtbaar als geannuleerd)")
    EventResponse.Admin cancel(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable UUID mosqueId,
                               @PathVariable UUID eventId) {
        return EventResponse.Admin.from(eventService.cancel(actor, mosqueId, eventId));
    }

    @DeleteMapping("/{eventId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Concept verwijderen")
    void delete(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable UUID mosqueId,
                @PathVariable UUID eventId) {
        eventService.delete(actor, mosqueId, eventId);
    }

    @GetMapping("/{eventId}/registrations")
    @Operation(summary = "Aanmeldingen voor de activiteit (alleen beheerders, bevat persoonsgegevens)")
    List<RegistrantView> registrations(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable UUID mosqueId,
                                       @PathVariable UUID eventId) {
        return registrationService.registrants(actor, mosqueId, eventId);
    }
}
