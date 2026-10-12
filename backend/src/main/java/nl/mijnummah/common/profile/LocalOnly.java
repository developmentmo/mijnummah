package nl.mijnummah.common.profile;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

/**
 * Guards settings that are only meant for a developer machine, such as {@code MAIL_PROVIDER=log}, which writes
 * password reset codes to the log. Outside the {@code local} and {@code test} profiles the application refuses to
 * start with such a setting, so it cannot end up in production by accident.
 */
public final class LocalOnly {

    private static final Profiles ALLOWED = Profiles.of("local", "test");

    private LocalOnly() {
    }

    public static void require(Environment environment, String setting) {
        if (!environment.acceptsProfiles(ALLOWED)) {
            throw new IllegalStateException(setting + " is only allowed with the local or test profile");
        }
    }
}
