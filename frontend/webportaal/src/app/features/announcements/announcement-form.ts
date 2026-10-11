import { AbstractControl, FormBuilder, ValidationErrors, Validators } from '@angular/forms';

import { AdminAnnouncement, AnnouncementRequest } from '../../core/api/models';
import { fromZonedParts, toZonedParts } from '../../core/format/dates';

/** Een vervaldatum zonder tijd verloopt aan het eind van die dag; een tijd zonder datum is onvolledig. */
function expiryComplete(group: AbstractControl): ValidationErrors | null {
  const v = group.value as { expiresDate: string; expiresTime: string };
  return v.expiresTime && !v.expiresDate ? { expiryDateMissing: true } : null;
}

export function createAnnouncementForm(fb: FormBuilder) {
  return fb.nonNullable.group(
    {
      title: ['', [Validators.required, Validators.maxLength(150)]],
      body: ['', [Validators.required, Validators.maxLength(5000)]],
      featured: [false],
      expiresDate: [''],
      expiresTime: [''],
    },
    { validators: expiryComplete },
  );
}

export type AnnouncementForm = ReturnType<typeof createAnnouncementForm>;

export function announcementToFormValue(announcement: AdminAnnouncement, timeZone: string) {
  const expires = announcement.expiresAt ? toZonedParts(announcement.expiresAt, timeZone) : null;
  return {
    title: announcement.title,
    body: announcement.body,
    featured: announcement.featured,
    expiresDate: expires?.date ?? '',
    expiresTime: expires?.time ?? '',
  };
}

export function formToAnnouncementRequest(
  form: AnnouncementForm,
  timeZone: string,
): AnnouncementRequest {
  const v = form.getRawValue();
  return {
    title: v.title.trim(),
    body: v.body.trim(),
    featured: v.featured,
    expiresAt: v.expiresDate
      ? fromZonedParts(v.expiresDate, v.expiresTime || '23:59', timeZone)
      : null,
  };
}
