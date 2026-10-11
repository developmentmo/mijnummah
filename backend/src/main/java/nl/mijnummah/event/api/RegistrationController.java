package nl.mijnummah.event.api;

import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import nl.mijnummah.common.web.PageRequests;
import nl.mijnummah.common.web.PageResponse;
import nl.mijnummah.event.application.RegistrationService;
import nl.mijnummah.event.application.RegistrationView;
import nl.mijnummah.identity.AuthenticatedUser;

/** Registrations of the signed-in user. */
@RestController
@Tag(name = "Aanmeldingen")
class RegistrationController {

    private final RegistrationService registrationService;

    RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping("/api/v1/events/{eventId}/registrations")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Aanmelden voor een activiteit")
    RegistrationView register(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable UUID eventId) {
        return registrationService.register(actor, eventId);
    }

    @DeleteMapping("/api/v1/events/{eventId}/registrations/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eigen aanmelding annuleren")
    void cancel(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable UUID eventId) {
        registrationService.cancel(actor, eventId);
    }

    @GetMapping("/api/v1/me/event-registrations")
    @Operation(summary = "Mijn aanmeldingen")
    PageResponse<RegistrationView> mine(@AuthenticationPrincipal AuthenticatedUser actor,
                                        @RequestParam(required = false) Integer page,
                                        @RequestParam(required = false) Integer size) {
        var pageable = PageRequests.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id")));
        return PageResponse.of(registrationService.mine(actor, pageable), view -> view);
    }
}
