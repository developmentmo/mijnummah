package nl.mijnummah.identity.application;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import nl.mijnummah.identity.UserDirectory;
import nl.mijnummah.identity.UserSummary;
import nl.mijnummah.identity.domain.User;
import nl.mijnummah.identity.infrastructure.UserRepository;

@Service
class UserDirectoryService implements UserDirectory {

    private final UserRepository users;

    UserDirectoryService(UserRepository users) {
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserSummary> findActiveByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return users.findByEmail(User.normalizeEmail(email)).filter(User::isActive).map(UserDirectoryService::summary);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, UserSummary> findByIds(Collection<UUID> ids) {
        return users.findAllById(ids).stream()
                .map(UserDirectoryService::summary)
                .collect(Collectors.toUnmodifiableMap(UserSummary::id, Function.identity()));
    }

    private static UserSummary summary(User user) {
        return new UserSummary(user.getId(), user.getEmail(), user.getDisplayName());
    }
}
