/**
 * Shared kernel: cross-cutting technical building blocks (error model, time source, rate limiting, paging).
 * <p>
 * Declared OPEN so every module may use its sub-packages. It must never contain business concepts
 * (mosques, events, media, ...); anything with domain meaning belongs in a feature module.
 */
@ApplicationModule(displayName = "Common", type = ApplicationModule.Type.OPEN)
package nl.mijnummah.common;

import org.springframework.modulith.ApplicationModule;
