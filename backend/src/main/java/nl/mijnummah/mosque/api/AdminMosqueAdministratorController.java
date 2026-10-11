package nl.mijnummah.mosque.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import nl.mijnummah.mosque.MosqueRole;
import nl.mijnummah.mosque.application.AdministratorView;
import nl.mijnummah.mosque.application.MosqueAdministratorService;

@RestController
@RequestMapping("/api/v1/admin/mosques/{mosqueId}/administrators")
@Tag(name = "Beheer: beheerders van een moskee")
class AdminMosqueAdministratorController {

    private final MosqueAdministratorService administratorService;

    AdminMosqueAdministratorController(MosqueAdministratorService administratorService) {
        this.administratorService = administratorService;
    }

    @GetMapping
    @Operation(summary = "Beheerders en redacteuren van de moskee")
    List<AdministratorResponse> list(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable UUID mosqueId) {
        return administratorService.list(actor, mosqueId).stream().map(AdministratorResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Bestaand account een rol geven voor deze moskee")
    AdministratorResponse add(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable UUID mosqueId,
                              @Valid @RequestBody AddAdministratorRequest request) {
        return AdministratorResponse.from(administratorService.add(actor, mosqueId, request.email(), request.role()));
    }

    @PutMapping("/{administratorId}")
    @Operation(summary = "Rol wijzigen")
    AdministratorResponse changeRole(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable UUID mosqueId,
                                     @PathVariable UUID administratorId, @Valid @RequestBody RoleRequest request) {
        return AdministratorResponse.from(administratorService.changeRole(actor, mosqueId, administratorId,
                request.role()));
    }

    @DeleteMapping("/{administratorId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Rechten voor deze moskee intrekken")
    void remove(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable UUID mosqueId,
                @PathVariable UUID administratorId) {
        administratorService.remove(actor, mosqueId, administratorId);
    }

    record AddAdministratorRequest(@NotBlank @Email @Size(max = 254) String email, @NotNull MosqueRole role) {
    }

    record RoleRequest(@NotNull MosqueRole role) {
    }

    record AdministratorResponse(UUID id, UUID userId, String email, String displayName, MosqueRole role,
                                 Instant createdAt) {

        static AdministratorResponse from(AdministratorView view) {
            return new AdministratorResponse(view.id(), view.userId(), view.email(), view.displayName(), view.role(),
                    view.createdAt());
        }
    }
}
