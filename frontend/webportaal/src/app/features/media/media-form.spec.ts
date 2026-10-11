import { TestBed } from '@angular/core/testing';
import { FormBuilder } from '@angular/forms';
import { describe, expect, it } from 'vitest';

import { aMedia } from '../../testing/harness';
import { createMediaForm, formToMediaRequest, mediaToFormValue } from './media-form';

describe('mediaformulier', () => {
  const form = () => createMediaForm(TestBed.inject(FormBuilder));

  it('herkent YouTube-links en losse video-ID’s', () => {
    const f = form();
    for (const ok of [
      'https://www.youtube.com/watch?v=dQw4w9WgXcQ',
      'https://youtu.be/dQw4w9WgXcQ',
      'dQw4w9WgXcQ',
    ]) {
      f.controls.videoUrl.setValue(ok);
      expect(f.controls.videoUrl.valid).toBe(true);
    }
    f.controls.videoUrl.setValue('https://vimeo.com/123');
    expect(f.controls.videoUrl.hasError('youtube')).toBe(true);
  });

  it('controleert de duur', () => {
    const f = form();
    f.controls.duration.setValue('1:05:00');
    expect(f.controls.duration.valid).toBe(true);
    f.controls.duration.setValue('ongeveer een uur');
    expect(f.controls.duration.hasError('duration')).toBe(true);
    f.controls.duration.setValue('0');
    expect(f.controls.duration.hasError('duration')).toBe(true);
  });

  it('zet het formulier om naar een verzoek en terug', () => {
    const f = form();
    f.reset(mediaToFormValue(aMedia({ relatedEventId: 'e-1' })));
    expect(f.controls.duration.value).toBe('1:05:00');
    expect(formToMediaRequest(f)).toMatchObject({
      videoUrl: 'https://www.youtube.com/watch?v=dQw4w9WgXcQ',
      durationSeconds: 3900,
      recordedOn: '2026-10-09',
      relatedEventId: 'e-1',
      thumbnailUrl: null,
    });
  });
});
