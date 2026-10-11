package nl.mijnummah.identity.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import nl.mijnummah.identity.AuthenticatedUser;
import nl.mijnummah.identity.application.AccountService;

@RestController
@RequestMapping("/api/v1/me")
@Tag(name = "Eigen account")
class AccountController {

    private final AccountService accountService;

    AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PutMapping("/password")
    @Operation(summary = "Wachtwoord wijzigen; alle sessies vervallen en dit antwoord bevat een nieuw tokenpaar")
    TokenResponse changePassword(@AuthenticationPrincipal AuthenticatedUser actor,
                                 @Valid @RequestBody ChangePasswordRequest request) {
        return TokenResponse.from(accountService.changePassword(actor, request.currentPassword(),
                request.newPassword()));
    }

    @PostMapping("/deletion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Account definitief verwijderen, met het wachtwoord als bevestiging",
            description = "Verwijdert ook volgen, meldingsvoorkeuren, apparaten en aanmeldingen. 409 "
                    + "ACCOUNT_DELETION_BLOCKED voor accounts die een moskee beheren.")
    void delete(@AuthenticationPrincipal AuthenticatedUser actor, @Valid @RequestBody DeleteAccountRequest request) {
        accountService.delete(actor, request.password());
    }

    record ChangePasswordRequest(@NotBlank @Size(max = 200) String currentPassword,
                                 @NotBlank @Size(max = 200) String newPassword) {

        @Override
        public String toString() {
            return "ChangePasswordRequest[***]";
        }
    }

    record DeleteAccountRequest(@NotBlank @Size(max = 200) String password) {

        @Override
        public String toString() {
            return "DeleteAccountRequest[***]";
        }
    }
}
