package nl.mijnummah.media.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class YouTubeVideoProviderTest {

    private static final String ID = "dQw4w9WgXcQ";

    private final YouTubeVideoProvider provider = new YouTubeVideoProvider();

    @ParameterizedTest
    @ValueSource(strings = {
            "dQw4w9WgXcQ",
            " dQw4w9WgXcQ ",
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=42s",
            "https://www.youtube.com/watch?feature=share&v=dQw4w9WgXcQ",
            "http://youtube.com/watch?v=dQw4w9WgXcQ",
            "www.youtube.com/watch?v=dQw4w9WgXcQ",
            "https://m.youtube.com/watch?v=dQw4w9WgXcQ",
            "https://music.youtube.com/watch?v=dQw4w9WgXcQ",
            "https://youtu.be/dQw4w9WgXcQ",
            "https://youtu.be/dQw4w9WgXcQ?si=abc123",
            "https://www.youtube.com/embed/dQw4w9WgXcQ",
            "https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ",
            "https://www.youtube.com/shorts/dQw4w9WgXcQ",
            "https://www.youtube.com/live/dQw4w9WgXcQ?feature=share"})
    void recognisesCommonYouTubeLinks(String input) {
        assertThat(provider.parseVideoId(input)).contains(ID);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "   ",
            "dQw4w9WgXc",
            "dQw4w9WgXcQQ",
            "https://vimeo.com/123456789",
            "https://www.youtube.com/channel/UC1234567890",
            "https://www.youtube.com/watch?v=short",
            "https://evil.example/watch?v=dQw4w9WgXcQ",
            "https://youtube.com.evil.example/watch?v=dQw4w9WgXcQ",
            "javascript:alert(1)",
            "ftp://youtube.com/watch?v=dQw4w9WgXcQ"})
    void rejectsAnythingElse(String input) {
        assertThat(provider.parseVideoId(input)).isEmpty();
    }

    @Test
    void rejectsNull() {
        assertThat(provider.parseVideoId(null)).isEmpty();
    }

    @Test
    void buildsPrivacyEnhancedEmbedAndWatchUrls() {
        assertThat(provider.embedUrl(ID)).isEqualTo("https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ");
        assertThat(provider.watchUrl(ID)).isEqualTo("https://www.youtube.com/watch?v=dQw4w9WgXcQ");
        assertThat(provider.defaultThumbnailUrl(ID)).isEqualTo("https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg");
    }

    @Test
    void registryResolvesToAReferenceOrRejectsWithAStableCode() {
        VideoProviders providers = VideoProviders.defaults();
        assertThat(providers.resolve("https://youtu.be/" + ID)).isEqualTo(new VideoReference(VideoProviderType.YOUTUBE, ID));
        assertThatThrownBy(() -> providers.resolve("https://vimeo.com/1"))
                .isInstanceOf(InvalidMediaException.class)
                .extracting("code").isEqualTo("INVALID_VIDEO_URL");
    }
}
