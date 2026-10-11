package nl.mijnummah.identity.domain;

public enum UserStatus {
    ACTIVE,
    /** Cannot sign in; existing sessions stop working at the next request. */
    BLOCKED
}
