package nl.mijnummah.media.domain;

/** Who may see a published media item. */
public enum Visibility {
    /** Everyone, also without an account. */
    PUBLIC,
    /** Only signed-in users. */
    REGISTERED_USERS
}
