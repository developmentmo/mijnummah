import { TestBed } from '@angular/core/testing';
import { FormBuilder } from '@angular/forms';
import { beforeEach, describe, expect, it } from 'vitest';

import { anEvent } from '../../testing/harness';
import { createEventForm, eventToFormValue, formToEventRequest } from './event-form';

describe('activiteitformulier', () => {
  let form: ReturnType<typeof createEventForm>;

  beforeEach(() => {
    form = createEventForm(TestBed.inject(FormBuilder));
  });

  function fill(values: Partial<ReturnType<typeof form.getRawValue>>) {
    form.patchValue({
      title: 'Lezing',
      category: 'LECTURE',
      startDate: '2026-11-06',
      startTime: '18:30',
      ...values,
    });
  }

  it('vraagt titel, categorie en begintijd', () => {
    expect(form.controls.title.hasError('required')).toBe(true);
    expect(form.controls.category.hasError('required')).toBe(true);
    expect(form.controls.startDate.hasError('required')).toBe(true);
    fill({});
    expect(form.valid).toBe(true);
  });

  it('weigert een einde voor het begin en een einddatum zonder tijd', () => {
    fill({ endTime: '17:00' });
    expect(form.hasError('endBeforeStart')).toBe(true);
    fill({ endTime: '20:00' });
    expect(form.valid).toBe(true);
    fill({ endDate: '2026-11-07', endTime: '' });
    expect(form.hasError('endTimeMissing')).toBe(true);
    fill({ endDate: '2026-11-07', endTime: '01:00' });
    expect(form.valid).toBe(true);
  });

  it('accepteert alleen https-afbeeldingen en een maximum van minstens 1', () => {
    fill({ imageUrl: 'http://example.org/a.jpg', capacity: 0 });
    expect(form.controls.imageUrl.hasError('https')).toBe(true);
    expect(form.controls.capacity.hasError('min')).toBe(true);
  });

  it('maakt een verzoek in UTC, met lege velden als null', () => {
    fill({ endTime: '20:00', location: '  ', description: 'Over geduld' });
    expect(formToEventRequest(form, 'Europe/Amsterdam')).toEqual({
      title: 'Lezing',
      description: 'Over geduld',
      category: 'LECTURE',
      startsAt: '2026-11-06T17:30:00Z',
      endsAt: '2026-11-06T19:00:00Z',
      location: null,
      imageUrl: null,
      visibility: 'PUBLIC',
      registrationEnabled: false,
      capacity: null,
    });
  });

  it('stuurt het maximum alleen mee als aanmelden aan staat', () => {
    fill({ registrationEnabled: false, capacity: 30 });
    expect(formToEventRequest(form, 'Europe/Amsterdam').capacity).toBeNull();
    fill({ registrationEnabled: true, capacity: 30 });
    expect(formToEventRequest(form, 'Europe/Amsterdam').capacity).toBe(30);
  });

  it('vult het formulier met lokale tijden uit een activiteit', () => {
    const value = eventToFormValue(anEvent(), 'Europe/Amsterdam');
    expect(value).toMatchObject({
      startDate: '2026-11-06',
      startTime: '18:30',
      endDate: '2026-11-06',
      endTime: '20:00',
      capacity: 40,
    });
  });
});
