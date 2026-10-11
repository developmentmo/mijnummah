package nl.mijnummah.media.domain;

import java.util.Optional;

/**
 * Adapter for one external video platform. Implementations only parse and build URLs; they never download,
 * store or proxy video content, and make no network calls.
 */
public interface VideoProvider {

    VideoProviderType type();

    /** The provider's video ID if {@code input} (a URL or a bare ID) refers to a video of this provider. */
    Optional<String> parseVideoId(String input);

    /** Page where the video can be watched on the provider's site. */
    String watchUrl(String videoId);

    /** URL for an embedded player (e.g. in a WebView or iframe). */
    String embedUrl(String videoId);

    /** Thumbnail to show when the administrator did not set one. */
    String defaultThumbnailUrl(String videoId);
}
