package nl.mijnummah.media.api;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

import nl.mijnummah.media.application.MediaInput;
import nl.mijnummah.media.domain.MediaCategory;
import nl.mijnummah.media.domain.Visibility;

record MediaRequest(
        @NotBlank @Size(max = 150) String title,
        @Size(max = 10000) String description,
        @Size(max = 100) String speaker,
        @NotNull MediaCategory category,
        @Schema(description = "YouTube-link of video-ID, bijv. https://www.youtube.com/watch?v=dQw4w9WgXcQ")
        @NotBlank @Size(max = 500) String videoUrl,
        @Size(max = 500) @Pattern(regexp = "^https://\\S+$", message = "must be an https URL") String thumbnailUrl,
        @Min(1) Integer durationSeconds,
        LocalDate recordedOn,
        UUID relatedEventId,
        Visibility visibility) {

    MediaInput toInput() {
        return new MediaInput(title, description, speaker, category, videoUrl, thumbnailUrl, durationSeconds,
                recordedOn, relatedEventId, visibility);
    }
}
