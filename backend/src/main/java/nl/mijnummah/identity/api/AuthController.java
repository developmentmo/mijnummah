package nl.mijnummah.identity.api;

import jakarta.validation.Valid;
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
import nl.mijnummah.identity.application.UserAdminService;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authenticatie")
class AuthController {

    private final AuthService authService;
    private final UserAdminService userAdminService;

    AuthController(AuthService authService, UserAdminService userAdminService) {
        this.authService = authService;
        this.userAdminService = userAdminService;
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

    record RefreshTokenRequest(@NotBlank @Size(max = 200) String refreshToken) {

        @Override
        public String toString() {
            return "RefreshTokenRequest[***]";
        }
    }
}
