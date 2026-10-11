/**
 * Datum en tijd in de tijdzone van de moskee (standaard Europe/Amsterdam), ongeacht de tijdzone van de computer
 * van de beheerder. De API werkt met UTC-instants; formulieren met een lokale datum en tijd.
 */

export const DEFAULT_TIME_ZONE = 'Europe/Amsterdam';

const formatters = new Map<string, Intl.DateTimeFormat>();

function formatter(timeZone: string, options: Intl.DateTimeFormatOptions): Intl.DateTimeFormat {
  const key = timeZone + JSON.stringify(options);
  let result = formatters.get(key);
  if (!result) {
    result = new Intl.DateTimeFormat('nl-NL', { timeZone, ...options });
    formatters.set(key, result);
  }
  return result;
}

/** "vr 6 nov 2026, 18:30" */
export function formatDateTime(iso: string, timeZone = DEFAULT_TIME_ZONE): string {
  const date = new Date(iso);
  return `${formatDate(iso, timeZone)}, ${formatter(timeZone, { hour: '2-digit', minute: '2-digit', hourCycle: 'h23' }).format(date)}`;
}

/** "vr 6 nov 2026" */
export function formatDate(iso: string, timeZone = DEFAULT_TIME_ZONE): string {
  return formatter(timeZone, {
    weekday: 'short',
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  })
    .format(new Date(iso))
    .replace(/\./g, '');
}

/** "18:30" */
export function formatTime(iso: string, timeZone = DEFAULT_TIME_ZONE): string {
  return formatter(timeZone, { hour: '2-digit', minute: '2-digit', hourCycle: 'h23' }).format(
    new Date(iso),
  );
}

/** Een kalenderdatum zonder tijd (yyyy-mm-dd, bijv. opnamedatum): "6 november 2026". */
export function formatCalendarDate(isoDate: string): string {
  const [year, month, day] = isoDate.split('-').map(Number);
  return formatter('UTC', { day: 'numeric', month: 'long', year: 'numeric' }).format(
    new Date(Date.UTC(year, month - 1, day)),
  );
}

/** Verschil in minuten tussen de lokale tijd in `timeZone` en UTC op moment `utcMs`. */
function offsetMinutes(utcMs: number, timeZone: string): number {
  const parts = formatter(timeZone, {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hourCycle: 'h23',
  }).formatToParts(new Date(utcMs));
  const get = (type: Intl.DateTimeFormatPartTypes) =>
    Number(parts.find((part) => part.type === type)?.value);
  const asUtc = Date.UTC(
    get('year'),
    get('month') - 1,
    get('day'),
    get('hour'),
    get('minute'),
    get('second'),
  );
  return Math.round((asUtc - utcMs) / 60000);
}

/** Lokale datum (yyyy-mm-dd) en tijd (HH:mm) in `timeZone` voor een instant, voor formuliervelden. */
export function toZonedParts(
  iso: string,
  timeZone = DEFAULT_TIME_ZONE,
): { date: string; time: string } {
  const utcMs = new Date(iso).getTime();
  const local = new Date(utcMs + offsetMinutes(utcMs, timeZone) * 60000);
  const pad = (n: number) => String(n).padStart(2, '0');
  return {
    date: `${local.getUTCFullYear()}-${pad(local.getUTCMonth() + 1)}-${pad(local.getUTCDate())}`,
    time: `${pad(local.getUTCHours())}:${pad(local.getUTCMinutes())}`,
  };
}

/**
 * Lokale datum en tijd in `timeZone` naar een UTC-instant (ISO-string). Bij de zomertijdovergang bestaat een
 * tijd als 02:30 niet; die schuift dan een uur op, zoals een klok dat ook doet.
 */
export function fromZonedParts(date: string, time: string, timeZone = DEFAULT_TIME_ZONE): string {
  const [year, month, day] = date.split('-').map(Number);
  const [hour, minute] = time.split(':').map(Number);
  const wallClock = Date.UTC(year, month - 1, day, hour, minute);
  const firstGuess = wallClock - offsetMinutes(wallClock, timeZone) * 60000;
  const utcMs = wallClock - offsetMinutes(firstGuess, timeZone) * 60000;
  return new Date(utcMs).toISOString().replace('.000Z', 'Z');
}

/** Seconden als "1:05:00" of "45:00", voor het duurveld. */
export function formatDuration(seconds: number | null): string {
  if (seconds == null) return '';
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  const s = seconds % 60;
  const pad = (n: number) => String(n).padStart(2, '0');
  return h > 0 ? `${h}:${pad(m)}:${pad(s)}` : `${m}:${pad(s)}`;
}

/** "1:05:00", "45:00" of "45" (minuten) naar seconden; null bij lege invoer, NaN bij ongeldige invoer. */
export function parseDuration(text: string): number | null {
  const value = text.trim();
  if (!value) return null;
  if (/^\d+$/.test(value)) return Number(value) * 60;
  const withHours = /^(\d+):([0-5]\d):([0-5]\d)$/.exec(value);
  if (withHours) {
    return Number(withHours[1]) * 3600 + Number(withHours[2]) * 60 + Number(withHours[3]);
  }
  const minutes = /^(\d+):([0-5]\d)$/.exec(value);
  return minutes ? Number(minutes[1]) * 60 + Number(minutes[2]) : NaN;
}
