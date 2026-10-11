/**
 * Lectures and other recordings of a mosque. Videos are never stored by this system: a media item only references
 * a video at an external provider through the {@link nl.mijnummah.media.domain.VideoProvider} interface (YouTube
 * first, see D-009). No live streaming.
 */
@ApplicationModule(displayName = "Media")
package nl.mijnummah.media;

import org.springframework.modulith.ApplicationModule;
