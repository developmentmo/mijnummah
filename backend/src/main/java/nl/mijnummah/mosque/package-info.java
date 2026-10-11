/**
 * The tenant: mosques, their public profile, and who may manage them (mosque administrators with role ADMIN or
 * EDITOR).
 * <p>
 * Public API for other modules: {@link MosqueAccessService} (every admin action on mosque data passes through
 * it), {@link MosqueDirectory} (resolving a public slug to an active mosque) and the types they return.
 */
@ApplicationModule(displayName = "Mosque")
package nl.mijnummah.mosque;

import org.springframework.modulith.ApplicationModule;
