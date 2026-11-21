package nl.mijnummah.mosque;

/** Whether a mosque is visible in the app and who may manage it. Set by a platform administrator. */
public enum MosqueStatus {

    /** Visible in the app; its administrators manage it. */
    ACTIVE,

    /** Hidden from the app, for example while it is being set up. Its administrators can prepare content. */
    INACTIVE,

    /** Temporarily blocked by the platform: hidden from the app, and only platform administrators can manage it. */
    BLOCKED
}
