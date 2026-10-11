package nl.mijnummah.mosque.api;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import nl.mijnummah.identity.AuthenticatedUser;
import nl.mijnummah.mosque.application.MosqueService;

@RestController
@RequestMapping("/api/v1/admin/mosques")
@Tag(name = "Beheer: moskeeën")
class AdminMosqueController {

    private final MosqueService mosqueService;

    AdminMosqueController(MosqueService mosqueService) {
        this.mosqueService = mosqueService;
    }

    @GetMapping
    @Operation(summary = "De moskeeën die ik beheer, met mijn rol")
    List<MosqueResponse.Admin> administered(@AuthenticationPrincipal AuthenticatedUser actor) {
        return mosqueService.administered(actor).stream().map(MosqueResponse.Admin::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Moskee aanmaken (alleen platformbeheerder)")
    MosqueResponse.Admin create(@AuthenticationPrincipal AuthenticatedUser actor,
                                @Valid @RequestBody CreateMosqueRequest request) {
        return MosqueResponse.Admin.from(mosqueService.create(actor, request.slug(), request.details().toDetails()));
    }

    @GetMapping("/{mosqueId}")
    @Operation(summary = "Moskee ophalen (beheerder of redacteur van deze moskee)")
    MosqueResponse.Admin get(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable UUID mosqueId) {
        return MosqueResponse.Admin.from(mosqueService.get(actor, mosqueId));
    }

    @PutMapping("/{mosqueId}")
    @Operation(summary = "Gegevens van de moskee wijzigen (beheerder van deze moskee)")
    MosqueResponse.Admin update(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable UUID mosqueId,
                                @Valid @RequestBody MosqueDetailsRequest request) {
        return MosqueResponse.Admin.from(mosqueService.update(actor, mosqueId, request.toDetails()));
    }

    @PutMapping("/{mosqueId}/active")
    @Operation(summary = "Moskee activeren of deactiveren (alleen platformbeheerder)")
    MosqueResponse.Admin setActive(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable UUID mosqueId,
                                   @Valid @RequestBody ActiveRequest request) {
        return MosqueResponse.Admin.from(mosqueService.setActive(actor, mosqueId, request.active()));
    }

    record CreateMosqueRequest(
            @NotBlank @Size(max = 60)
            @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$", message = "must be lower-case letters, digits and hyphens")
            String slug,
            @NotNull @Valid MosqueDetailsRequest details) {
    }

    record ActiveRequest(@NotNull Boolean active) {
    }
}
