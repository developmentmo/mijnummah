package nl.mijnummah.mosque;

/**
 * Configuration of a mosque, set by the platform administrator.
 *
 * @param registrationsEnabled users may register for the mosque's events (each event also has its own switch)
 * @param pushEnabled          the mosque's administrators may send push notifications
 */
public record MosqueSettingsView(boolean registrationsEnabled, boolean pushEnabled) {

    /** For a new mosque when nothing else is given. */
    public static final MosqueSettingsView DEFAULTS = new MosqueSettingsView(true, true);
}
