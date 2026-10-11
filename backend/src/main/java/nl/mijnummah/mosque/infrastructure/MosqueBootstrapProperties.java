package nl.mijnummah.mosque.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * First mosque, created at startup when slug and name are set (BOOTSTRAP_MOSQUE_SLUG / BOOTSTRAP_MOSQUE_NAME) and
 * no mosque with that slug exists. When {@code adminEmail} names an existing account, it becomes ADMIN of that
 * mosque. An existing mosque is never changed.
 */
@ConfigurationProperties(prefix = "mijnummah.bootstrap.mosque")
public record MosqueBootstrapProperties(String slug, String name, String city, String adminEmail) {

    public boolean configured() {
        return slug != null && !slug.isBlank() && name != null && !name.isBlank();
    }
}
