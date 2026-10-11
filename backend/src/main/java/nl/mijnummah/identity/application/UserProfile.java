package nl.mijnummah.identity.application;

import java.time.Instant;
import java.util.UUID;

import nl.mijnummah.identity.PlatformRole;
import nl.mijnummah.identity.domain.User;
import nl.mijnummah.identity.domain.UserStatus;

public record UserProfile(UUID id, String email, String displayName, PlatformRole platformRole, UserStatus status,
                          Instant createdAt) {

    public static UserProfile of(User user) {
        return new UserProfile(user.getId(), user.getEmail(), user.getDisplayName(), user.getPlatformRole(),
                user.getStatus(), user.getCreatedAt());
    }
}
