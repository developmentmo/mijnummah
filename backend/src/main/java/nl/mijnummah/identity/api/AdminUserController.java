package nl.mijnummah.identity.api;

import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import nl.mijnummah.common.web.PageRequests;
import nl.mijnummah.common.web.PageResponse;
import nl.mijnummah.identity.AuthenticatedUser;
import nl.mijnummah.identity.PlatformRole;
import nl.mijnummah.identity.application.UserAdminService;
import nl.mijnummah.identity.domain.UserStatus;

/** Platform administrators only (checked in {@link UserAdminService}). */
@RestController
@RequestMapping("/api/v1/admin/users")
@Tag(name = "Platformbeheer: accounts")
class AdminUserController {

    private final UserAdminService userAdminService;

    AdminUserController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Account aanmaken (bijv. voor een moskeebeheerder); zonder wachtwoord krijgt het account een "
            + "uitnodiging per e-mail om zelf een wachtwoord te kiezen")
    UserResponse create(@AuthenticationPrincipal AuthenticatedUser actor, @Valid @RequestBody CreateUserRequest request) {
        return UserResponse.from(userAdminService.create(actor, request.email(), request.displayName(),
                request.password(), request.platformRole()));
    }

    @GetMapping
    @Operation(summary = "Accounts, te zoeken op e-mailadres of naam en te filteren op status")
    PageResponse<UserResponse> list(@AuthenticationPrincipal AuthenticatedUser actor,
                                    @RequestParam(required = false) @Size(max = 100) String q,
                                    @RequestParam(required = false) UserStatus status,
                                    @RequestParam(required = false) Integer page,
                                    @RequestParam(required = false) Integer size) {
        var pageable = PageRequests.of(page, size, Sort.by("email"));
        return PageResponse.of(userAdminService.list(actor, q, status, pageable), UserResponse::from);
    }

    @PostMapping("/{userId}/password-code")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Code per e-mail sturen om een nieuw wachtwoord in te stellen (72 uur geldig)")
    void sendPasswordCode(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable UUID userId) {
        userAdminService.sendPasswordCode(actor, userId);
    }

    @PostMapping("/{userId}/block")
    @Operation(summary = "Account blokkeren; alle sessies vervallen direct")
    UserResponse block(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable UUID userId) {
        return UserResponse.from(userAdminService.block(actor, userId));
    }

    @PostMapping("/{userId}/unblock")
    @Operation(summary = "Blokkade opheffen")
    UserResponse unblock(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable UUID userId) {
        return UserResponse.from(userAdminService.unblock(actor, userId));
    }

    record CreateUserRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(max = 100) String displayName,
            /** Null: an invitation email lets the user choose a password. */
            @Size(min = 1, max = 200) String password,
            @NotNull PlatformRole platformRole) {

        @Override
        public String toString() {
            return "CreateUserRequest[email=" + email + ", platformRole=" + platformRole + "]";
        }
    }
}
