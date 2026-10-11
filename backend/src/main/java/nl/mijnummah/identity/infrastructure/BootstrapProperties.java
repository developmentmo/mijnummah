package nl.mijnummah.identity.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * First platform administrator, created at startup when no platform administrator exists yet and both email and
 * password are set (BOOTSTRAP_ADMIN_EMAIL / BOOTSTRAP_ADMIN_PASSWORD). Remove the password from the environment
 * after the first start; an existing administrator is never changed.
 */
@ConfigurationProperties(prefix = "mijnummah.bootstrap.admin")
public record BootstrapProperties(String email, String password, String displayName) {

    public boolean configured() {
        return email != null && !email.isBlank() && password != null && !password.isBlank();
    }

    public String displayNameOrDefault() {
        return displayName == null || displayName.isBlank() ? "Platformbeheerder" : displayName;
    }

    @Override
    public String toString() {
        return "BootstrapProperties[email=" + email + "]"; // never print the password
    }
}
