/**
 * Identity and access: user accounts, platform roles, authentication (JWT access tokens and rotating refresh
 * tokens) and the HTTP security configuration.
 * <p>
 * Rights per mosque (ADMIN, EDITOR) are not part of this module: they belong to the mosque module, which owns
 * the tenant. Other modules may only use the types in this base package ({@link AuthenticatedUser},
 * {@link PlatformRole}, {@link UserDirectory}, {@link UserSummary}); sub-packages are internal.
 */
@ApplicationModule(displayName = "Identity")
package nl.mijnummah.identity;

import org.springframework.modulith.ApplicationModule;
