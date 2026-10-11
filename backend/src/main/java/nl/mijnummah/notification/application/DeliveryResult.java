package nl.mijnummah.notification.application;

public enum DeliveryResult {

    /** Accepted by the push service. */
    DELIVERED,

    /** The token is no longer valid (app removed, token rotated); the device is deleted. */
    INVALID_TOKEN,

    /** Temporarily not deliverable (rate limit, outage); counted as failed, the device is kept. */
    FAILED
}
