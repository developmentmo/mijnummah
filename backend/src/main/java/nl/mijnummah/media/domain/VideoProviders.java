package nl.mijnummah.media.domain;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** All supported providers. Resolves what an administrator pasted into a {@link VideoReference}. */
public final class VideoProviders {

    private final Map<VideoProviderType, VideoProvider> byType;

    public VideoProviders(List<VideoProvider> providers) {
        this.byType = providers.stream().collect(Collectors.toUnmodifiableMap(VideoProvider::type, Function.identity()));
    }

    public static VideoProviders defaults() {
        return new VideoProviders(List.of(new YouTubeVideoProvider()));
    }

    /** @throws InvalidMediaException {@code INVALID_VIDEO_URL} when no provider recognises the input */
    public VideoReference resolve(String videoUrl) {
        for (VideoProvider provider : byType.values()) {
            var id = provider.parseVideoId(videoUrl);
            if (id.isPresent()) {
                return new VideoReference(provider.type(), id.get());
            }
        }
        throw new InvalidMediaException("INVALID_VIDEO_URL",
                "videoUrl is not a supported video link. Use a YouTube link or video ID.");
    }

    public VideoProvider get(VideoProviderType type) {
        VideoProvider provider = byType.get(type);
        if (provider == null) {
            throw new IllegalStateException("No video provider for " + type);
        }
        return provider;
    }
}
