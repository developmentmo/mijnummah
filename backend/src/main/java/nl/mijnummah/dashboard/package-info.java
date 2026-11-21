/**
 * Platform statistics for the platform administrator: totals and counts per mosque.
 * <p>
 * Read-only reporting over the tables of other modules with plain SQL (D-058): counting is not domain logic, and a
 * count method in every module would add API surface for this one screen. Nothing depends on this module, and it
 * never writes.
 */
@ApplicationModule(displayName = "Dashboard")
package nl.mijnummah.dashboard;

import org.springframework.modulith.ApplicationModule;
