package nl.mijnummah.identity.infrastructure;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Browser origins that may call the API, e.g. the admin portal ({@code https://beheer.example.nl}).
 * Empty (the default) means no cross-origin access; mobile apps are not affected by CORS.
 */
@ConfigurationProperties(prefix = "mijnummah.cors")
public record CorsProperties(List<String> allowedOrigins) {

    public CorsProperties {
        allowedOrigins = allowedOrigins == null
                ? List.of()
                : allowedOrigins.stream().map(String::trim).filter(origin -> !origin.isEmpty()).toList();
        if (allowedOrigins.contains("*")) {
            throw new IllegalStateException("mijnummah.cors.allowed-origins must list explicit origins, not '*'");
        }
    }
}
