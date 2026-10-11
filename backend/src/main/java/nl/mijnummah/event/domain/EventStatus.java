package nl.mijnummah.event.domain;

/**
 * DRAFT: only visible to administrators. PUBLISHED: public according to its visibility. CANCELLED: stays
 * publicly visible with this status, so people who planned to come see the cancellation; final.
 */
public enum EventStatus {
    DRAFT,
    PUBLISHED,
    CANCELLED
}
