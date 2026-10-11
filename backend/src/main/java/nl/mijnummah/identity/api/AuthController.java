package nl.mijnummah.identity.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import nl.mijnummah.identity.AuthenticatedUser;
import nl.mijnummah.identity.application.AuthService;
import nl.mijnummah.identity.application.PasswordResetService;
import nl.mijnummah.identity.application.UserAdminService;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authenticatie")
class AuthController {

    private final AuthService authService;
    private final UserAdminService userAdminService;
    private final PasswordResetService passwordResetService;

    AuthController(AuthService authService, UserAdminService userAdminService,
                   PasswordResetService passwordResetService) {
        this.authService = authService;
        this.userAdminService = userAdminService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Account aanmaken in de app en direct inloggen",
            description = "Een nieuw account heeft geen beheerrechten. 409 EMAIL_ALREADY_REGISTERED als het adres al "
                    + "in gebruik is.")
    TokenResponse register(@Valid @RequestBody RegisterRequest request) {
        return TokenResponse.from(authService.register(request.email(), request.displayName(), request.password()));
    }

    @PostMapping("/password-reset")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Code aanvragen om het wachtwoord te herstellen",
            description = "Altijd 202, ook als er geen account met dit adres is. 503 PASSWORD_RESET_UNAVAILABLE als "
                    + "e-mail niet is ingesteld.")
    void requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        passwordResetService.request(request.email());
    }

    @PostMapping("/password-reset/confirm")
    @Operation(summary = "Nieuw wachtwoord instellen met de code uit de e-mail; logt in en beëindigt andere sessies")
    TokenResponse confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        return TokenResponse.from(passwordResetService.confirm(request.email(), request.code(), request.newPassword()));
    }

    @PostMapping("/login")
    @Operation(summary = "Inloggen met e-mail en wachtwoord")
    TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return TokenResponse.from(authService.login(request.email(), request.password()));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Nieuw tokenpaar met een refresh token (het oude refresh token vervalt)")
    TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return TokenResponse.from(authService.refresh(request.refreshToken()));
    }

    /** Public on purpose: possession of the refresh token is the authorization to end its session. */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Uitloggen: beëindigt de sessie van dit refresh token")
    void logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.refreshToken());
    }

    @GetMapping("/me")
    @Operation(summary = "Het eigen account")
    UserResponse me(@AuthenticationPrincipal AuthenticatedUser actor) {
        return UserResponse.from(userAdminService.profile(actor.id()));
    }

    record LoginRequest(@NotBlank @Size(max = 254) String email, @NotBlank @Size(max = 200) String password) {

        @Override
        public String toString() {
            return "LoginRequest[email=" + email + "]";
        }
    }

    record RegisterRequest(@NotBlank @Email @Size(max = 254) String email,
                           @NotBlank @Size(max = 100) String displayName,
                           @NotBlank @Size(max = 200) String password) {

        @Override
        public String toString() {
            return "RegisterRequest[email=" + email + "]";
        }
    }

    record PasswordResetRequest(@NotBlank @Email @Size(max = 254) String email) {
    }

    record PasswordResetConfirmRequest(@NotBlank @Size(max = 254) String email,
                                       @NotBlank @Size(max = 20) String code,
                                       @NotBlank @Size(max = 200) String newPassword) {

        @Override
        public String toString() {
            return "PasswordResetConfirmRequest[email=" + email + "]";
        }
    }

    record RefreshTokenRequest(@NotBlank @Size(max = 200) String refreshToken) {

        @Override
        public String toString() {
            return "RefreshTokenRequest[***]";
        }
    }
}
