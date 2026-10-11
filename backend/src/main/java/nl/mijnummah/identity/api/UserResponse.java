package nl.mijnummah.identity.api;

import java.time.Instant;
import java.util.UUID;

import nl.mijnummah.identity.PlatformRole;
import nl.mijnummah.identity.application.UserProfile;
import nl.mijnummah.identity.domain.UserStatus;

record UserResponse(UUID id, String email, String displayName, PlatformRole platformRole, UserStatus status,
                    Instant createdAt) {

    static UserResponse from(UserProfile profile) {
        return new UserResponse(profile.id(), profile.email(), profile.displayName(), profile.platformRole(),
                profile.status(), profile.createdAt());
    }
}
