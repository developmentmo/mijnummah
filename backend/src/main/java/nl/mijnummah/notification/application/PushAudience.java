package nl.mijnummah.notification.application;

/**
 * Who a push notification would reach right now.
 *
 * @param configured whether a push service is set up
 * @param enabledForMosque false when the platform administrator switched push off for this mosque
 * @param simulated  local development: notifications are only logged, never delivered
 * @param followers  active followers who did not switch the topic off
 * @param devices    their registered devices (one person can have several)
 */
public record PushAudience(boolean configured, boolean simulated, boolean enabledForMosque, int followers,
                           int devices) {
}
