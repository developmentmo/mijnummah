import { HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { describe, expect, it } from 'vitest';

import { ApiError, describeError, translateFieldMessage } from './api-error';

describe('ApiError', () => {
  it('leest code, melding, veldfouten en Retry-After uit het backendantwoord', () => {
    const error = ApiError.from(
      new HttpErrorResponse({
        status: 429,
        error: {
          status: 429,
          code: 'TOO_MANY_LOGIN_ATTEMPTS',
          message: 'Too many',
          fieldErrors: [],
        },
        headers: new HttpHeaders({ 'Retry-After': '120' }),
      }),
    );
    expect(error.code).toBe('TOO_MANY_LOGIN_ATTEMPTS');
    expect(error.status).toBe(429);
    expect(error.retryAfterSeconds).toBe(120);
  });

  it('maakt van status 0 een netwerkfout', () => {
    const error = ApiError.from(new HttpErrorResponse({ status: 0 }));
    expect(error.isNetworkError).toBe(true);
    expect(describeError(error)).toContain('Geen verbinding');
  });

  it('valt terug op HTTP_<status> als de body geen code heeft', () => {
    expect(ApiError.from(new HttpErrorResponse({ status: 502, error: 'Bad gateway' })).code).toBe(
      'HTTP_502',
    );
  });
});

describe('describeError', () => {
  const conflict = new ApiError(409, 'EVENT_HAS_REGISTRATIONS', 'x');

  it('geeft een schermspecifieke melding voorrang', () => {
    expect(describeError(conflict, { EVENT_HAS_REGISTRATIONS: 'Er zijn aanmeldingen.' })).toBe(
      'Er zijn aanmeldingen.',
    );
  });

  it('kent algemene codes en servers die falen', () => {
    expect(describeError(new ApiError(403, 'MOSQUE_ACCESS_DENIED', 'x'))).toContain('geen rechten');
    expect(describeError(new ApiError(500, 'WHATEVER', 'x'))).toContain('server');
    expect(describeError(conflict)).toContain('Er ging iets mis');
  });
});

describe('translateFieldMessage', () => {
  it('vertaalt de validatiemeldingen van de backend', () => {
    expect(translateFieldMessage('must not be blank')).toBe('Vul dit veld in.');
    expect(translateFieldMessage('must be an https URL')).toContain('https://');
    expect(translateFieldMessage('size must be between 0 and 150')).toBe('Deze tekst is te lang.');
    expect(translateFieldMessage('iets onbekends')).toBe('Deze waarde wordt niet geaccepteerd.');
  });
});
