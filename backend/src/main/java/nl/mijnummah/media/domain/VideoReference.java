package nl.mijnummah.media.domain;

import java.util.Objects;

/** A video at an external provider. */
public record VideoReference(VideoProviderType provider, String videoId) {

    public VideoReference {
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(videoId, "videoId");
    }
}
