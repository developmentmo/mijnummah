package nl.mijnummah.notification;

/** Progress of a push notification. */
public enum PushStatus {

    /** Accepted; the push service is being called for every device. */
    SENDING,

    /** Done: every device was attempted (some may have failed, see the counts). */
    SENT,

    /** The push service could not be used at all (e.g. rejected credentials). */
    FAILED
}
