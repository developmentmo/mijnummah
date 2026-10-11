/**
 * Announcements (short news items) of a mosque. Publishing an announcement only makes it visible in the app; it
 * never sends a push notification. Sending one is a separate, explicit action through the notification module.
 */
@ApplicationModule(displayName = "Announcement")
package nl.mijnummah.announcement;

import org.springframework.modulith.ApplicationModule;
