package nl.mijnummah.identity;

import java.util.Objects;
import java.util.UUID;

/**
 * The authenticated caller, as seen by every other module.
 * <p>
 * Built per request from the database (not from token claims), so blocking a user or changing their role takes
 * effect immediately. Controllers obtain it with {@code @AuthenticationPrincipal} and pass it explicitly to
 * application services, which perform their own authorization checks.
 */
public record AuthenticatedUser(UUID id, String email, PlatformRole platformRole) {

    public AuthenticatedUser {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(platformRole, "platformRole");
    }

    public boolean isPlatformAdmin() {
        return platformRole == PlatformRole.PLATFORM_ADMIN;
    }
}
