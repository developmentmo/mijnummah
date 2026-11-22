import { AbstractControl, FormBuilder, ValidationErrors, Validators } from '@angular/forms';

import { AdminEvent, EventCategory, EventRequest, Visibility } from '../../core/api/models';
import { fromZonedParts, toZonedParts } from '../../core/format/dates';
import { blankToNull, httpsUrl } from '../../shared/forms/form-errors';

/** Einde (als ingevuld) moet na het begin liggen. Een eindtijd zonder einddatum valt op de begindag. */
export function endAfterStart(group: AbstractControl): ValidationErrors | null {
  const v = group.value as {
    startDate: string;
    startTime: string;
    endDate: string;
    endTime: string;
  };
  if (!v.startDate || !v.startTime || (!v.endDate && !v.endTime)) return null;
  if (!v.endTime) return { endTimeMissing: true };
  const start = `${v.startDate}T${v.startTime}`;
  const end = `${v.endDate || v.startDate}T${v.endTime}`;
  return end > start ? null : { endBeforeStart: true };
}

export function createEventForm(fb: FormBuilder) {
  return fb.nonNullable.group(
    {
      title: ['', [Validators.required, Validators.maxLength(150)]],
      description: ['', Validators.maxLength(10000)],
      category: fb.control<EventCategory | null>(null, Validators.required),
      startDate: ['', Validators.required],
      startTime: ['', Validators.required],
      endDate: [''],
      endTime: [''],
      location: ['', Validators.maxLength(200)],
      // Oudere activiteiten kunnen een externe link hebben; nieuwe afbeeldingen worden geüpload.
      imageUrl: ['', [Validators.maxLength(500), httpsUrl]],
      imageId: fb.control<string | null>(null),
      visibility: ['PUBLIC' as Visibility],
      registrationEnabled: [false],
      capacity: fb.control<number | null>(null, Validators.min(1)),
    },
    { validators: endAfterStart },
  );
}

export type EventForm = ReturnType<typeof createEventForm>;

export function eventToFormValue(event: AdminEvent, timeZone: string) {
  const start = toZonedParts(event.startsAt, timeZone);
  const end = event.endsAt ? toZonedParts(event.endsAt, timeZone) : null;
  return {
    title: event.title,
    description: event.description ?? '',
    category: event.category,
    startDate: start.date,
    startTime: start.time,
    endDate: end?.date ?? '',
    endTime: end?.time ?? '',
    location: event.location ?? '',
    imageUrl: event.externalImageUrl ?? '',
    imageId: event.imageId,
    visibility: event.visibility,
    registrationEnabled: event.registrationEnabled,
    capacity: event.capacity,
  };
}

export function formToEventRequest(form: EventForm, timeZone: string): EventRequest {
  const v = form.getRawValue();
  return {
    title: v.title.trim(),
    description: blankToNull(v.description),
    category: v.category!,
    startsAt: fromZonedParts(v.startDate, v.startTime, timeZone),
    endsAt: v.endTime ? fromZonedParts(v.endDate || v.startDate, v.endTime, timeZone) : null,
    location: blankToNull(v.location),
    imageUrl: v.imageId ? null : blankToNull(v.imageUrl),
    imageId: v.imageId,
    visibility: v.visibility,
    registrationEnabled: v.registrationEnabled,
    // De backend staat een maximum alleen toe als aanmelden aan staat.
    capacity: v.registrationEnabled ? v.capacity || null : null,
  };
}

/** Veldnamen van de backend die anders heten in het formulier. */
export const EVENT_FIELD_MAP: Record<string, string> = {
  startsAt: 'startDate',
  endsAt: 'endTime',
};
