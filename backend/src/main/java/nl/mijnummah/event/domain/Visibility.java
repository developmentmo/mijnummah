package nl.mijnummah.event.domain;

/** Who may see published content. */
public enum Visibility {
    /** Everyone, also without an account. */
    PUBLIC,
    /** Only signed-in users. */
    REGISTERED_USERS
}
