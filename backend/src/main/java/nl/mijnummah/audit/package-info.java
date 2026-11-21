/**
 * Audit log: who performed which important management action, when, and for which mosque.
 * <p>
 * Modules publish a {@link nl.mijnummah.common.audit.AuditEvent}; this module stores it in the same transaction.
 * No other module depends on this one. Reading the log: platform administrators see everything, an administrator
 * of a mosque sees the entries of that mosque.
 */
@ApplicationModule(displayName = "Audit")
package nl.mijnummah.audit;

import org.springframework.modulith.ApplicationModule;
