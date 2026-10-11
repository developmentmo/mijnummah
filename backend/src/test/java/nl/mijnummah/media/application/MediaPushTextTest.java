package nl.mijnummah.media.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import nl.mijnummah.media.domain.MediaCategory;
import nl.mijnummah.media.domain.MediaDetails;
import nl.mijnummah.media.domain.VideoProviderType;
import nl.mijnummah.media.domain.VideoReference;
import nl.mijnummah.media.domain.Visibility;

class MediaPushTextTest {

    @Test
    void namesTheKindOfVideoAndTheSpeaker() {
        assertThat(MediaAdminService.defaultPushBody(details(MediaCategory.LECTURE, "Imam Yusuf")))
                .isEqualTo("Lezing van Imam Yusuf");
        assertThat(MediaAdminService.defaultPushBody(details(MediaCategory.KHUTBAH, null)))
                .isEqualTo("Vrijdagpreek nu te bekijken in de app");
    }

    private static MediaDetails details(MediaCategory category, String speaker) {
        return new MediaDetails("Titel", null, speaker, category,
                new VideoReference(VideoProviderType.YOUTUBE, "dQw4w9WgXcQ"), null, null, null, null,
                Visibility.PUBLIC);
    }
}
