package nl.mijnummah.identity.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

import nl.mijnummah.identity.AuthenticatedUser;
import nl.mijnummah.identity.domain.User;

/**
 * Turns a validated JWT into an {@link AuthenticatedUserToken}.
 * <p>
 * Loads the user on every request (one primary-key lookup), so blocking a user takes effect immediately and the
 * role comes from the database, never from token claims.
 * <p>
 * Not a Spring bean on purpose: Boot would register every {@code Converter} bean with MVC's conversion service.
 */
class AuthenticatedUserConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserRepository users;

    AuthenticatedUserConverter(UserRepository users) {
        this.users = users;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        UUID userId = parseSubject(jwt);
        User user = users.findById(userId)
                .orElseThrow(() -> new InvalidBearerTokenException("Unknown token subject"));
        if (!user.isActive()) {
            throw new DisabledException("User account is not active");
        }
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getPlatformRole()));
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getEmail(), user.getPlatformRole());
        return new AuthenticatedUserToken(principal, jwt, authorities);
    }

    private static UUID parseSubject(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new InvalidBearerTokenException("Invalid token subject");
        }
    }
}
