package nl.mijnummah.common.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bound from {@code mijnummah.mail.*}. The SMTP connection itself is configured with Spring Boot's
 * {@code spring.mail.*} properties (environment variables {@code SPRING_MAIL_HOST}, {@code SPRING_MAIL_PORT},
 * {@code SPRING_MAIL_USERNAME}, {@code SPRING_MAIL_PASSWORD}).
 *
 * @param provider {@code none}, {@code smtp} or {@code log}
 * @param from     sender address, required for {@code smtp}
 */
@ConfigurationProperties(prefix = "mijnummah.mail")
public record MailProperties(Provider provider, String from) {

    public enum Provider { NONE, SMTP, LOG }

    public MailProperties {
        provider = provider == null ? Provider.NONE : provider;
        if (provider == Provider.SMTP && (from == null || from.isBlank())) {
            throw new IllegalStateException("mijnummah.mail.from (MAIL_FROM) is required when MAIL_PROVIDER=smtp");
        }
    }
}
