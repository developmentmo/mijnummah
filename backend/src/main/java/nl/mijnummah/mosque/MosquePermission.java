package nl.mijnummah.mosque;

/** What an administrator may do for a mosque. Which role grants what is defined in {@link MosqueRole}. */
public enum MosquePermission {

    /** Create, edit, publish and delete events, media items and announcements. */
    MANAGE_CONTENT,

    /** View the list of people registered for an event (personal data). */
    VIEW_REGISTRATIONS,

    /** Edit the mosque's own details (name, address, contact). */
    MANAGE_MOSQUE,

    /** Add and remove administrators of the mosque. */
    MANAGE_ADMINISTRATORS,

    /** Send a push notification about published content to the mosque's followers. */
    SEND_NOTIFICATIONS
}
