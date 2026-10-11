import { HttpErrorResponse } from '@angular/common/http';

export interface FieldError {
  field: string;
  message: string;
}

/** Een mislukte API-aanroep in de vorm die de backend teruggeeft: `{status, code, message, fieldErrors}`. */
export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly code: string,
    message: string,
    readonly fieldErrors: FieldError[] = [],
    /** Seconden uit de `Retry-After`-header, bij te veel pogingen. */
    readonly retryAfterSeconds?: number,
  ) {
    super(message);
    this.name = 'ApiError';
  }

  get isNetworkError(): boolean {
    return this.status === 0;
  }

  static from(error: unknown): ApiError {
    if (error instanceof ApiError) return error;
    if (error instanceof HttpErrorResponse) {
      if (error.status === 0) {
        return new ApiError(0, 'NETWORK', 'Geen verbinding met de server.');
      }
      const body = error.error as Partial<{
        code: string;
        message: string;
        fieldErrors: FieldError[];
      }>;
      const retryAfter = Number(error.headers?.get('Retry-After'));
      return new ApiError(
        error.status,
        typeof body?.code === 'string' ? body.code : `HTTP_${error.status}`,
        typeof body?.message === 'string' ? body.message : error.message,
        Array.isArray(body?.fieldErrors) ? body.fieldErrors : [],
        Number.isFinite(retryAfter) && retryAfter > 0 ? retryAfter : undefined,
      );
    }
    return new ApiError(-1, 'UNKNOWN', error instanceof Error ? error.message : String(error));
  }
}

/** Nederlandse meldingen voor foutcodes die op meerdere schermen kunnen voorkomen. */
const COMMON_MESSAGES: Record<string, string> = {
  NETWORK:
    'Geen verbinding met de server. Controleer je internetverbinding en probeer het opnieuw.',
  UNAUTHORIZED: 'Je sessie is verlopen. Log opnieuw in.',
  FORBIDDEN: 'Je hebt geen rechten voor deze actie.',
  MOSQUE_ACCESS_DENIED: 'Je hebt geen rechten voor deze actie bij deze moskee.',
  MOSQUE_NOT_FOUND: 'Deze moskee bestaat niet (meer).',
  NOT_FOUND: 'Dit item bestaat niet (meer).',
  EVENT_NOT_FOUND: 'Deze activiteit bestaat niet (meer).',
  MEDIA_NOT_FOUND: 'Dit media-item bestaat niet (meer).',
  ANNOUNCEMENT_NOT_FOUND: 'Deze aankondiging bestaat niet (meer).',
  VALIDATION_FAILED: 'Controleer de gemarkeerde velden.',
  CONCURRENT_MODIFICATION:
    'Iemand anders heeft dit tegelijk gewijzigd. Laad de pagina opnieuw en probeer het nog eens.',
  RATE_LIMITED: 'Te veel verzoeken achter elkaar. Wacht even en probeer het opnieuw.',
  INTERNAL_ERROR: 'Er ging iets mis op de server. Probeer het later opnieuw.',
};

/**
 * Zet een fout om in een Nederlandse melding. `specific` gaat voor op de algemene meldingen, zodat een scherm
 * zijn eigen uitleg kan geven (bijv. bij `EVENT_HAS_REGISTRATIONS`).
 */
export function describeError(error: unknown, specific: Record<string, string> = {}): string {
  const apiError = ApiError.from(error);
  return (
    specific[apiError.code] ??
    COMMON_MESSAGES[apiError.code] ??
    (apiError.status >= 500
      ? COMMON_MESSAGES['INTERNAL_ERROR']
      : 'Er ging iets mis. Probeer het opnieuw of neem contact op met de beheerder.')
  );
}

/** Vertaalt de (Engelstalige) validatiemeldingen van de backend naar korte Nederlandse teksten. */
export function translateFieldMessage(message: string): string {
  if (/must not be (blank|null|empty)/.test(message)) return 'Vul dit veld in.';
  if (/https URL/.test(message)) return 'Gebruik een link die met https:// begint.';
  if (/http\(s\) URL/.test(message)) return 'Gebruik een link die met http:// of https:// begint.';
  if (/well-formed email/.test(message)) return 'Vul een geldig e-mailadres in.';
  if (/size must be between/.test(message)) return 'Deze tekst is te lang.';
  if (/greater than or equal to/.test(message)) return 'Deze waarde is te laag.';
  if (/two-letter country code/.test(message))
    return 'Gebruik een landcode van twee letters, bijv. NL.';
  return 'Deze waarde wordt niet geaccepteerd.';
}
