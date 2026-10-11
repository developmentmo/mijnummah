package nl.mijnummah.identity;

/** Platform-wide role. Rights for a specific mosque are granted separately (mosque administrators). */
public enum PlatformRole {

    /** Operates the platform: creates mosques, accounts and the first administrators of a mosque. */
    PLATFORM_ADMIN,

    /** Everyone else. May follow mosques and register for events; may be an administrator of specific mosques. */
    USER
}
