import { describe, expect, it } from 'vitest';

import {
  formatCalendarDate,
  formatDate,
  formatDateTime,
  formatDuration,
  formatTime,
  fromZonedParts,
  parseDuration,
  toZonedParts,
} from './dates';

describe('datums in de tijdzone van de moskee', () => {
  it('zet een lokale wintertijd om naar UTC (UTC+1)', () => {
    expect(fromZonedParts('2026-11-06', '18:30', 'Europe/Amsterdam')).toBe('2026-11-06T17:30:00Z');
  });

  it('zet een lokale zomertijd om naar UTC (UTC+2)', () => {
    expect(fromZonedParts('2026-07-01', '18:30', 'Europe/Amsterdam')).toBe('2026-07-01T16:30:00Z');
  });

  it('gaat goed rond de overgang naar wintertijd', () => {
    // 25 oktober 2026: om 03:00 zomertijd gaat de klok terug naar 02:00.
    expect(fromZonedParts('2026-10-25', '12:00', 'Europe/Amsterdam')).toBe('2026-10-25T11:00:00Z');
    expect(fromZonedParts('2026-10-24', '12:00', 'Europe/Amsterdam')).toBe('2026-10-24T10:00:00Z');
  });

  it('geeft formuliervelden in de tijdzone van de moskee, ook over middernacht UTC', () => {
    expect(toZonedParts('2026-11-06T23:30:00Z', 'Europe/Amsterdam')).toEqual({
      date: '2026-11-07',
      time: '00:30',
    });
  });

  it('heen en terug levert hetzelfde moment op', () => {
    const iso = '2026-03-29T08:15:00Z';
    const { date, time } = toZonedParts(iso, 'Europe/Amsterdam');
    expect(fromZonedParts(date, time, 'Europe/Amsterdam')).toBe(iso);
  });

  it('toont Nederlandse datums en tijden zonder punten in afkortingen', () => {
    expect(formatDate('2026-11-06T17:30:00Z')).toBe('vr 6 nov 2026');
    expect(formatTime('2026-11-06T17:30:00Z')).toBe('18:30');
    expect(formatDateTime('2026-11-06T17:30:00Z')).toBe('vr 6 nov 2026, 18:30');
    expect(formatCalendarDate('2026-10-09')).toBe('9 oktober 2026');
  });
});

describe('duur van een video', () => {
  it('leest u:mm:ss, mm:ss en losse minuten', () => {
    expect(parseDuration('1:05:00')).toBe(3900);
    expect(parseDuration('45:30')).toBe(2730);
    expect(parseDuration('90:00')).toBe(5400);
    expect(parseDuration('45')).toBe(2700);
    expect(parseDuration('  ')).toBeNull();
  });

  it('herkent ongeldige invoer', () => {
    expect(parseDuration('1:5')).toBeNaN();
    expect(parseDuration('een uur')).toBeNaN();
    expect(parseDuration('1:60:00')).toBeNaN();
  });

  it('toont seconden als tekst die het formulier weer kan lezen', () => {
    expect(formatDuration(3900)).toBe('1:05:00');
    expect(formatDuration(2730)).toBe('45:30');
    expect(formatDuration(null)).toBe('');
    expect(parseDuration(formatDuration(3725)!)).toBe(3725);
  });
});
