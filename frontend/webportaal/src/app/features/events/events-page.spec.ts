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
  respond,
  selectMosque,
  spyOnRouter,
  text,
} from '../../testing/harness';
import { EventsPage } from './events-page';

const LIST = `${V1}/admin/mosques/${MOSQUE.id}/events`;

describe('EventsPage', () => {
  beforeEach(async () => {
    sessionStorage.clear();
    configureTesting();
    await selectMosque();
  });

  afterEach(() => http().verify());

  async function render(inputs: Record<string, string> = {}) {
    const fixture = TestBed.createComponent(EventsPage);
    for (const [k, v] of Object.entries(inputs)) fixture.componentRef.setInput(k, v);
    await fixture.whenStable();
    return fixture;
  }

  it('toont activiteiten met tijd in Amsterdam, status en aanmeldingen', async () => {
    const fixture = await render();
    const request = await respond(
      (r) => r.url === LIST,
      page([
        anEvent({ status: 'PUBLISHED' }),
        anEvent({
          id: 'e-2',
          title: 'Concept',
          registrationEnabled: false,
          visibility: 'REGISTERED_USERS',
        }),
      ]),
    );
    expect(request.request.params.get('size')).toBe('20');
    expect(request.request.params.has('status')).toBe(false);
    await fixture.whenStable();
    const rows = Array.from(
      fixture.nativeElement.querySelectorAll('tbody tr') as NodeListOf<HTMLElement>,
    ).map(text);
    expect(rows[0]).toContain('vr 6 nov 2026, 18:30');
    expect(rows[0]).toContain('Gepubliceerd');
    expect(rows[0]).toContain('12 / 40');
    expect(rows[1]).toContain('Alleen voor ingelogde gebruikers');
    expect(rows[1]).toContain('Concept');
  });

  it('filtert op de status uit de URL', async () => {
    await render({ status: 'DRAFT', pagina: '2' });
    const request = await respond((r) => r.url === LIST, page([], 0, 1));
    expect(request.request.params.get('status')).toBe('DRAFT');
    expect(request.request.params.get('page')).toBe('1');
  });

  it('negeert een onbekende status in de URL', async () => {
    await render({ status: 'ONZIN' });
    const request = await respond((r) => r.url === LIST, page([]));
    expect(request.request.params.has('status')).toBe(false);
  });

  it('toont een lege toestand met een vervolgstap', async () => {
    const router = spyOnRouter();
    const fixture = await render();
    await respond((r) => r.url === LIST, page([]));
    await fixture.whenStable();
    expect(text(fixture.nativeElement)).toContain('Nog geen activiteiten');
    (
      fixture.nativeElement.querySelector('mu-state-placeholder button') as HTMLButtonElement
    ).click();
    expect(router.navigate).toHaveBeenCalledWith(['/m', MOSQUE.id, 'agenda', 'nieuw']);
  });

  it('toont een fout met opnieuw proberen', async () => {
    const fixture = await render();
    const { body, options } = apiError('INTERNAL_ERROR', 500);
    await respond((r) => r.url === LIST, body, options);
    await fixture.whenStable();
    expect(text(fixture.nativeElement.querySelector('[role=alert]'))).toContain('server');
    (fixture.nativeElement.querySelector('[role=alert] button') as HTMLButtonElement).click();
    await respond((r) => r.url === LIST, page([anEvent()]));
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelectorAll('tbody tr').length).toBe(1);
  });
});
