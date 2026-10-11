import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';

import {
  MOSQUE,
  V1,
  anEvent,
  apiError,
  configureTesting,
  http,
  page,
  selectMosque,
  settle,
  text,
} from '../../testing/harness';
import { DashboardPage } from './dashboard-page';

const BASE = `${V1}/admin/mosques/${MOSQUE.id}`;

describe('DashboardPage', () => {
  beforeEach(async () => {
    sessionStorage.clear();
    configureTesting();
    await selectMosque();
  });

  afterEach(() => http().verify());

  it('toont alleen echte gegevens uit de API', async () => {
    const fixture = TestBed.createComponent(DashboardPage);
    await fixture.whenStable();
    await settle();

    const upcoming = http().expectOne(
      (r) => r.url === `${BASE}/events` && r.params.get('status') === 'PUBLISHED',
    );
    expect(upcoming.request.params.get('from')).toMatch(/^\d{4}-\d{2}-\d{2}T/);
    expect(upcoming.request.params.get('size')).toBe('5');
    upcoming.flush(page([anEvent({ status: 'PUBLISHED' })], 7));

    http()
      .expectOne((r) => r.url === `${BASE}/events` && r.params.get('status') === 'DRAFT')
      .flush(page([], 2));
    http()
      .expectOne((r) => r.url === `${BASE}/media` && r.params.get('status') === 'PUBLISHED')
      .flush(page([], 14));
    http()
      .expectOne((r) => r.url === `${BASE}/media` && r.params.get('status') === 'DRAFT')
      .flush(page([], 1));
    const { body, options } = apiError('INTERNAL_ERROR', 500);
    http()
      .expectOne((r) => r.url === `${BASE}/announcements`)
      .flush(body, options);
    await settle();
    await fixture.whenStable();

    const content = text(fixture.nativeElement);
    expect(content).toContain('7 gepubliceerde activiteiten gepland');
    expect(content).toContain('12 van 40 plaatsen bezet');
    const tiles = Array.from(
      fixture.nativeElement.querySelectorAll('.tile') as NodeListOf<HTMLElement>,
    ).map(text);
    expect(tiles[0]).toMatch(/^2\s*Concepten in de agenda/);
    expect(tiles[1]).toMatch(/^14\s*Gepubliceerde video’s/);
    // Eén mislukte telling blokkeert de rest niet en krijgt een eigen herstelactie.
    expect(tiles[3]).toContain('Niet geladen');
    expect(tiles[3]).toContain('Opnieuw proberen');
  });
});
