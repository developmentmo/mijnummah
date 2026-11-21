package nl.mijnummah.mosque.api;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import nl.mijnummah.identity.AuthenticatedUser;
import nl.mijnummah.mosque.MosqueSettingsView;
import nl.mijnummah.mosque.MosqueStatus;
import nl.mijnummah.mosque.application.MosqueService;
import nl.mijnummah.mosque.application.MosqueService.CreatedMosque;
import nl.mijnummah.mosque.application.MosqueService.FirstAdministrator;
import nl.mijnummah.mosque.application.MosqueService.NewMosque;
import nl.mijnummah.storage.StoredImageRef;

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
    CreatedMosqueResponse create(@AuthenticationPrincipal AuthenticatedUser actor,
                                 @Valid @RequestBody CreateMosqueRequest request) {
        FirstAdministrator admin = request.firstAdministrator() == null ? null
                : new FirstAdministrator(request.firstAdministrator().email(),
                        request.firstAdministrator().displayName());
        CreatedMosque created = mosqueService.create(actor, new NewMosque(request.slug(),
                request.details().toDetails(), admin, request.settings()));
        return new CreatedMosqueResponse(MosqueResponse.Admin.from(created.mosque()), created.settings(),
                created.administratorUserId() == null ? null
                        : new CreatedMosqueResponse.Administrator(created.administratorUserId(),
                                created.administratorInvited()));
    }

    @GetMapping("/{mosqueId}/settings")
    @Operation(summary = "Configuratie van de moskee (beheerder of redacteur van deze moskee)")
    MosqueSettingsView settings(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable UUID mosqueId) {
        return mosqueService.settings(actor, mosqueId);
    }

    @PutMapping("/{mosqueId}/settings")
    @Operation(summary = "Configuratie van de moskee wijzigen (alleen platformbeheerder)")
    MosqueSettingsView updateSettings(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable UUID mosqueId,
                                      @Valid @RequestBody SettingsRequest request) {
        return mosqueService.updateSettings(actor, mosqueId,
                new MosqueSettingsView(request.registrationsEnabled(), request.pushEnabled()));
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

    @PatchMapping("/{mosqueId}/status")
    @Operation(summary = "Moskee activeren, deactiveren of blokkeren (alleen platformbeheerder)")
    MosqueResponse.Admin setStatus(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable UUID mosqueId,
                                   @Valid @RequestBody StatusRequest request) {
        return MosqueResponse.Admin.from(mosqueService.setStatus(actor, mosqueId, request.status()));
    }

    @PostMapping(path = "/{mosqueId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Afbeelding uploaden (JPEG of PNG, beheerder of redacteur van deze moskee)")
    StoredImageRef uploadImage(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable UUID mosqueId,
                               @RequestPart("file") MultipartFile file) {
        try {
            return mosqueService.uploadImage(actor, mosqueId, file.getBytes());
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @PutMapping("/{mosqueId}/images")
    @Operation(summary = "Logo en omslagfoto instellen of verwijderen (beheerder van deze moskee)")
    MosqueResponse.Admin setImages(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable UUID mosqueId,
                                   @RequestBody ImagesRequest request) {
        return MosqueResponse.Admin.from(mosqueService.setImages(actor, mosqueId, request.logoImageId(),
                request.coverImageId()));
    }

    /** Null removes the logo or cover photo. */
    record ImagesRequest(UUID logoImageId, UUID coverImageId) {
    }

    /** Without a slug, one is made from the name; without a first administrator, none is added. */
    record CreateMosqueRequest(
            @Size(max = 60)
            @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$", message = "must be lower-case letters, digits and hyphens")
            String slug,
            @NotNull @Valid MosqueDetailsRequest details,
            @Valid FirstAdministratorRequest firstAdministrator,
            MosqueSettingsView settings) {
    }

    /** An existing account gets the ADMIN role; for a new address an account is made and invited by email. */
    record FirstAdministratorRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(max = 100) String displayName) {

        @Override
        public String toString() {
            return "FirstAdministratorRequest[***]";
        }
    }

    record SettingsRequest(@NotNull Boolean registrationsEnabled, @NotNull Boolean pushEnabled) {
    }

    /** @param firstAdministrator null when none was asked for */
    record CreatedMosqueResponse(MosqueResponse.Admin mosque, MosqueSettingsView settings,
                                 Administrator firstAdministrator) {

        /** @param invited true when a new account was made; it receives an invitation email */
        record Administrator(UUID userId, boolean invited) {
        }
    }

    record StatusRequest(@NotNull MosqueStatus status) {
    }
}
