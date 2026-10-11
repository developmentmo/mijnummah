import { provideHttpClient, withInterceptors } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
  TestRequest,
} from '@angular/common/http/testing';
import { Provider } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { vi } from 'vitest';

import {
  AdminAnnouncement,
  AdminEvent,
  AdminMedia,
  AdminMosque,
  CurrentUser,
  Page,
  PushAudience,
  PushNotification,
} from '../core/api/models';
import { authInterceptor } from '../core/auth/auth.interceptor';
import { APP_CONFIG } from '../core/config/app-config';
import { MosqueContext } from '../core/mosque/mosque-context';

/** Hulpmiddelen voor de unit tests: echte services en interceptor, nep-HTTP via HttpTestingController. */

export const API = 'http://api.test';
export const V1 = `${API}/api/v1`;

export function configureTesting(providers: Provider[] = []): void {
  TestBed.configureTestingModule({
    providers: [
      provideHttpClient(withInterceptors([authInterceptor])),
      provideHttpClientTesting(),
      provideRouter([]),
      { provide: APP_CONFIG, useValue: { apiBaseUrl: API } },
      ...providers,
    ],
  });
}

export function http(): HttpTestingController {
  return TestBed.inject(HttpTestingController);
}

/** Laat openstaande promises en timers van nul afronden (de interceptor wacht op een token-promise). */
export async function settle(): Promise<void> {
  for (let i = 0; i < 3; i++) await new Promise((resolve) => setTimeout(resolve));
}

/** Wacht tot het verzoek er is, beantwoordt het en laat de gevolgen afronden. */
export async function respond(
  match: string | ((req: import('@angular/common/http').HttpRequest<unknown>) => boolean),
  body: unknown,
  options?: { status?: number; statusText?: string; headers?: Record<string, string> },
): Promise<TestRequest> {
  await settle();
  const request = http().expectOne(match);
  request.flush(body as never, options);
  await settle();
  return request;
}

export function apiError(code: string, status: number, fieldErrors: unknown[] = []) {
  return {
    body: { status, code, message: code, fieldErrors },
    options: { status, statusText: 'Error' },
  };
}

/** Router-navigatie vastleggen zonder echte routes. */
export function spyOnRouter() {
  const router = TestBed.inject(Router);
  return {
    navigate: vi.spyOn(router, 'navigate').mockResolvedValue(true),
    navigateByUrl: vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true),
  };
}

export const MOSQUE: AdminMosque = {
  id: 'm-1',
  slug: 'noeroel-islam',
  name: 'Noeroel Islam',
  description: 'Testmoskee',
  address: { street: 'Straat 1', postalCode: '1234 AB', city: 'Voorbeeldstad', countryCode: 'NL' },
  contact: { email: 'info@example.org', phone: null, websiteUrl: null },
  timeZone: 'Europe/Amsterdam',
  active: true,
  myRole: 'ADMIN',
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
};

export const USER: CurrentUser = {
  id: 'u-1',
  email: 'beheerder@example.org',
  displayName: 'Fatima',
  platformRole: 'USER',
  status: 'ACTIVE',
  createdAt: '2026-01-01T00:00:00Z',
};

/** Kiest een moskee in de context, zoals de mosqueGuard dat doet. */
export async function selectMosque(mosque: AdminMosque = MOSQUE): Promise<void> {
  const selecting = TestBed.inject(MosqueContext).select(mosque.id);
  await respond(`${V1}/admin/mosques`, [mosque]);
  await selecting;
}

export const AUDIENCE: PushAudience = {
  configured: true,
  simulated: false,
  followers: 12,
  devices: 15,
};

export function aPushNotification(overrides: Partial<PushNotification> = {}): PushNotification {
  return {
    id: 'p-1',
    mosqueId: MOSQUE.id,
    contentType: 'ANNOUNCEMENT',
    contentId: 'a-1',
    title: 'Iftar vrijdag',
    body: 'Iedereen is welkom.',
    status: 'SENT',
    recipientCount: 15,
    deliveredCount: 14,
    failedCount: 1,
    createdAt: '2026-10-11T10:00:00Z',
    completedAt: '2026-10-11T10:00:05Z',
    ...overrides,
  };
}

/** Beantwoordt de verzoeken van een pushpaneel: het bereik en de eventueel al verstuurde melding. */
export async function respondPushPanel(
  audience: PushAudience = AUDIENCE,
  sent: PushNotification | null = null,
): Promise<void> {
  await settle();
  for (const request of http().match((r) => r.url.endsWith('/notifications/audience'))) {
    request.flush(audience);
  }
  for (const request of http().match((r) => r.url.endsWith('/notifications'))) {
    request.flush(page(sent ? [sent] : []));
  }
  await settle();
}

export function page<T>(items: T[], totalItems = items.length, pageIndex = 0): Page<T> {
  return { items, page: pageIndex, size: 20, totalItems, totalPages: Math.ceil(totalItems / 20) };
}

export function anEvent(overrides: Partial<AdminEvent> = {}): AdminEvent {
  return {
    id: 'e-1',
    mosqueId: MOSQUE.id,
    title: 'Lezing over geduld',
    description: null,
    category: 'LECTURE',
    startsAt: '2026-11-06T17:30:00Z',
    endsAt: '2026-11-06T19:00:00Z',
    location: 'Grote zaal',
    imageUrl: null,
    visibility: 'PUBLIC',
    registrationEnabled: true,
    capacity: 40,
    confirmedRegistrations: 12,
    availablePlaces: 28,
    status: 'DRAFT',
    publishedAt: null,
    cancelledAt: null,
    createdAt: '2026-10-01T10:00:00Z',
    updatedAt: '2026-10-01T10:00:00Z',
    ...overrides,
  };
}

export function aMedia(overrides: Partial<AdminMedia> = {}): AdminMedia {
  return {
    id: 'v-1',
    mosqueId: MOSQUE.id,
    title: 'Khutbah over dankbaarheid',
    description: null,
    speaker: 'Imam Yusuf',
    category: 'KHUTBAH',
    video: {
      provider: 'YOUTUBE',
      videoId: 'dQw4w9WgXcQ',
      watchUrl: 'https://www.youtube.com/watch?v=dQw4w9WgXcQ',
      embedUrl: 'https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ',
    },
    thumbnailUrl: null,
    customThumbnailUrl: null,
    durationSeconds: 3900,
    recordedOn: '2026-10-09',
    relatedEventId: null,
    visibility: 'PUBLIC',
    status: 'DRAFT',
    publishedAt: null,
    createdAt: '2026-10-01T10:00:00Z',
    updatedAt: '2026-10-01T10:00:00Z',
    ...overrides,
  };
}

export function anAnnouncement(overrides: Partial<AdminAnnouncement> = {}): AdminAnnouncement {
  return {
    id: 'a-1',
    mosqueId: MOSQUE.id,
    title: 'Iftar op zaterdag',
    body: 'Iedereen is welkom.',
    featured: false,
    status: 'DRAFT',
    expired: false,
    publishedAt: null,
    expiresAt: null,
    createdAt: '2026-10-01T10:00:00Z',
    updatedAt: '2026-10-01T10:00:00Z',
    ...overrides,
  };
}

/** Tekst van een element, met samengevoegde witruimte. */
export function text(element: Element | null): string {
  return (element?.textContent ?? '').replace(/\s+/g, ' ').trim();
}

export function buttonByText(root: HTMLElement, label: string): HTMLButtonElement {
  const button = Array.from(root.querySelectorAll<HTMLButtonElement>('button, a')).find(
    (b) => text(b) === label,
  );
  if (!button) throw new Error(`Knop niet gevonden: ${label}`);
  return button as HTMLButtonElement;
}

export function typeInto(input: HTMLInputElement | HTMLTextAreaElement, value: string): void {
  input.value = value;
  input.dispatchEvent(new Event('input'));
  input.dispatchEvent(new Event('blur'));
}
