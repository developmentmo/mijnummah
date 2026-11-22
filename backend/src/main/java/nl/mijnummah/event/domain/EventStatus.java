package nl.mijnummah.event.domain;

/**
 * DRAFT: only visible to administrators. PUBLISHED: public according to its visibility. CANCELLED: stays
 * publicly visible with this status, so people who planned to come see the cancellation; final. ARCHIVED: hidden
 * from the app and the public API, kept for the administration; can be restored.
 */
public enum EventStatus {
    DRAFT,
    PUBLISHED,
    CANCELLED,
    ARCHIVED
}
