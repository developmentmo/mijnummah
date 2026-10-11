package nl.mijnummah.notification.application;

/**
 * The push service (Firebase Cloud Messaging, which also delivers to iOS through APNs). Implementations live in
 * the infrastructure package; which one is active is decided by {@code mijnummah.push.provider}.
 */
public interface PushGateway {

    /** Whether notifications can actually be delivered. */
    boolean configured();

    /** True for the local-development gateway that only logs: nothing reaches a phone. */
    default boolean simulated() {
        return false;
    }

    /**
     * Delivers one message to one device.
     *
     * @throws PushServiceException when the push service cannot be used at all (credentials, configuration); a
     *                              problem with this one device is a {@link DeliveryResult} instead
     */
    DeliveryResult send(PushMessage message, String token);
}
