package nl.mijnummah.media.domain;

/** Supported external video platforms. Adding one means: a value here, a {@link VideoProvider}, and a migration
 * that extends {@code ck_media_item_video_provider}. */
public enum VideoProviderType {
    YOUTUBE
}
