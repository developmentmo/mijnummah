import { FormBuilder, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';

import { AdminMedia, MediaCategory, MediaRequest, Visibility } from '../../core/api/models';
import { formatDuration, parseDuration } from '../../core/format/dates';
import { blankToNull, httpsUrl, youtubeUrl } from '../../shared/forms/form-errors';

export const durationText: ValidatorFn = (control): ValidationErrors | null => {
  const seconds = parseDuration(String(control.value ?? ''));
  return seconds === null || (Number.isFinite(seconds) && seconds > 0) ? null : { duration: true };
};

export function createMediaForm(fb: FormBuilder) {
  return fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(150)]],
    speaker: ['', Validators.maxLength(100)],
    category: fb.control<MediaCategory | null>(null, Validators.required),
    videoUrl: ['', [Validators.required, Validators.maxLength(500), youtubeUrl]],
    description: ['', Validators.maxLength(10000)],
    // Oudere video's kunnen een externe link hebben; nieuwe thumbnails worden geüpload.
    thumbnailUrl: ['', [Validators.maxLength(500), httpsUrl]],
    thumbnailImageId: fb.control<string | null>(null),
    duration: ['', durationText],
    recordedOn: [''],
    relatedEventId: [''],
    visibility: ['PUBLIC' as Visibility],
  });
}

export type MediaForm = ReturnType<typeof createMediaForm>;

export function mediaToFormValue(media: AdminMedia) {
  return {
    title: media.title,
    speaker: media.speaker ?? '',
    category: media.category,
    videoUrl: media.video.watchUrl,
    description: media.description ?? '',
    thumbnailUrl: media.customThumbnailUrl ?? '',
    thumbnailImageId: media.thumbnailImageId,
    duration: formatDuration(media.durationSeconds),
    recordedOn: media.recordedOn ?? '',
    relatedEventId: media.relatedEventId ?? '',
    visibility: media.visibility,
  };
}

export function formToMediaRequest(form: MediaForm): MediaRequest {
  const v = form.getRawValue();
  return {
    title: v.title.trim(),
    speaker: blankToNull(v.speaker),
    category: v.category!,
    videoUrl: v.videoUrl.trim(),
    description: blankToNull(v.description),
    thumbnailUrl: v.thumbnailImageId ? null : blankToNull(v.thumbnailUrl),
    thumbnailImageId: v.thumbnailImageId,
    durationSeconds: parseDuration(v.duration),
    recordedOn: blankToNull(v.recordedOn),
    relatedEventId: blankToNull(v.relatedEventId),
    visibility: v.visibility,
  };
}

export const MEDIA_FIELD_MAP: Record<string, string> = { durationSeconds: 'duration' };
