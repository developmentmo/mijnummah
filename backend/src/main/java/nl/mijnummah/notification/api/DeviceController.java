package nl.mijnummah.notification.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
import nl.mijnummah.notification.application.DeviceService;
import nl.mijnummah.notification.domain.DevicePlatform;
import nl.mijnummah.notification.domain.PushDevice;

@RestController
@RequestMapping("/api/v1/me/devices")
@Tag(name = "Pushmeldingen: apparaten")
class DeviceController {

    private final DeviceService deviceService;

    DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @PutMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Pushtoken van dit apparaat registreren (idempotent)",
            description = "Een token hoort bij wie er op het apparaat is ingelogd; logt iemand anders in, dan gaat het "
                    + "token naar die persoon. mosqueSlug is de moskee van de app: alleen meldingen van die moskee "
                    + "gaan naar dit token.")
    void register(@AuthenticationPrincipal AuthenticatedUser actor, @Valid @RequestBody DeviceRequest request) {
        deviceService.register(actor, request.token(), request.platform(), request.mosqueSlug());
    }

    /** A POST with a body rather than a DELETE with the token in the URL, so tokens do not end up in access logs. */
    @PostMapping("/remove")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Pushtoken van dit apparaat verwijderen (bij uitloggen; idempotent)")
    void unregister(@AuthenticationPrincipal AuthenticatedUser actor, @Valid @RequestBody TokenRequest request) {
        deviceService.unregister(actor, request.token());
    }

    record DeviceRequest(@NotBlank @Size(max = PushDevice.MAX_TOKEN_LENGTH) String token,
                         @NotNull DevicePlatform platform,
                         @NotBlank @Size(max = 60) @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$") String mosqueSlug) {

        @Override
        public String toString() {
            return "DeviceRequest[platform=" + platform + ", mosqueSlug=" + mosqueSlug + "]";
        }
    }

    record TokenRequest(@NotBlank @Size(max = PushDevice.MAX_TOKEN_LENGTH) String token) {

        @Override
        public String toString() {
            return "TokenRequest[***]";
        }
    }
}
