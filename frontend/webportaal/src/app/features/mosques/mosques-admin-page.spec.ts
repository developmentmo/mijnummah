import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { MosqueCounts } from '../../core/api/models';
import { Confirm } from '../../shared/ui/confirm-dialog';
import {
  MOSQUE,
  V1,
  apiError,
  buttonByText,
  configureTesting,
  http,
  respond,
  text,
  typeInto,
} from '../../testing/harness';
import { MosquesAdminPage } from './mosques-admin-page';

const MOSQUES = `${V1}/admin/mosques`;
const OVERVIEW = `${V1}/admin/platform/mosques`;
const ROW: MosqueCounts = {
  id: MOSQUE.id,
  slug: MOSQUE.slug,
  name: MOSQUE.name,
  city: 'Voorbeeldstad',
  status: 'ACTIVE',
  createdAt: MOSQUE.createdAt,
  administrators: 2,
  followers: 140,
  events: 12,
  videos: 7,
  announcements: 3,
};
const OTHER: MosqueCounts = {
  ...ROW,
  id: 'm-2',
  slug: 'voorbeeldmoskee',
  name: 'Voorbeeldmoskee',
  city: 'Utrecht',
  status: 'INACTIVE',
  followers: 0,
};

describe('MosquesAdminPage', () => {
  const ask = vi.fn();

  beforeEach(() => {
    ask.mockReset();
    sessionStorage.clear();
    configureTesting([{ provide: Confirm, useValue: { ask } }]);
  });

  afterEach(() => http().verify());

  async function render() {
    const fixture = TestBed.createComponent(MosquesAdminPage);
    await fixture.whenStable();
    await respond(OVERVIEW, [ROW, OTHER]);
    await fixture.whenStable();
    return fixture;
  }

  /** Na een statuswijziging ververst de pagina ook de moskeeën van de kopbalk. */
  async function respondContext() {
    await respond((r) => r.url === MOSQUES && r.method === 'GET', [MOSQUE]);
  }

  it('toont alle moskeeën met hun status en aantallen', async () => {
    const fixture = await render();
    const el: HTMLElement = fixture.nativeElement;
    expect(text(el)).toContain('Noeroel Islam');
    expect(text(el)).toContain('voorbeeldmoskee');
    expect(text(el)).toContain('Actief');
    expect(text(el)).toContain('Inactief');
    expect(text(el.querySelector('tbody tr'))).toContain('140');
    expect(el.querySelectorAll('a[href="/m/m-2"]')).toHaveLength(1);
  });

  it('zoekt op naam of plaats en filtert op status', async () => {
    const fixture = await render();
    const el: HTMLElement = fixture.nativeElement;
    typeInto(el.querySelector<HTMLInputElement>('input[type=search]')!, 'utrecht');
    await fixture.whenStable();
    expect(el.querySelectorAll('tbody tr')).toHaveLength(1);
    expect(text(el)).toContain('1 van 2 moskeeën');
    typeInto(el.querySelector<HTMLInputElement>('input[type=search]')!, 'bestaat niet');
    await fixture.whenStable();
    expect(text(el)).toContain('Geen moskeeën gevonden.');
  });

  it('deactiveert pas na bevestiging en werkt de rij bij', async () => {
    ask.mockResolvedValue(true);
    const fixture = await render();
    buttonByText(fixture.nativeElement, 'Deactiveren').click();
    await fixture.whenStable();
    expect(ask).toHaveBeenCalledWith(
      expect.objectContaining({ title: 'Noeroel Islam deactiveren?', destructive: true }),
    );
    const request = await respond(`${MOSQUES}/m-1/status`, { ...MOSQUE, status: 'INACTIVE' });
    expect(request.request.method).toBe('PATCH');
    expect(request.request.body).toEqual({ status: 'INACTIVE' });
    await respondContext();
    await fixture.whenStable();
    expect(text(fixture.nativeElement)).not.toContain('Deactiveren');
  });

  it('blokkeert een moskee en heft de blokkade weer op', async () => {
    ask.mockResolvedValue(true);
    const fixture = await render();
    const el: HTMLElement = fixture.nativeElement;
    el.querySelectorAll('tbody tr')[0].querySelectorAll('button')[1].click();
    await fixture.whenStable();
    expect(ask).toHaveBeenCalledWith(
      expect.objectContaining({ title: 'Noeroel Islam blokkeren?', destructive: true }),
    );
    const request = await respond(`${MOSQUES}/m-1/status`, { ...MOSQUE, status: 'BLOCKED' });
    expect(request.request.body).toEqual({ status: 'BLOCKED' });
    await respondContext();
    await fixture.whenStable();
    expect(text(el)).toContain('Geblokkeerd');

    buttonByText(el, 'Blokkade opheffen').click();
    await fixture.whenStable();
    expect(ask).toHaveBeenLastCalledWith(
      expect.objectContaining({ title: 'Blokkade van Noeroel Islam opheffen?' }),
    );
    const lifted = await respond(`${MOSQUES}/m-1/status`, MOSQUE);
    expect(lifted.request.body).toEqual({ status: 'ACTIVE' });
    await respondContext();
  });

  it('doet niets als de bevestiging wordt geannuleerd', async () => {
    ask.mockResolvedValue(false);
    const fixture = await render();
    buttonByText(fixture.nativeElement, 'Activeren').click();
    await fixture.whenStable();
    http().expectNone((r) => r.method === 'PATCH');
  });

  it('meldt een fout bij het wijzigen van de status', async () => {
    ask.mockResolvedValue(true);
    const fixture = await render();
    buttonByText(fixture.nativeElement, 'Activeren').click();
    await fixture.whenStable();
    const error = apiError('MOSQUE_ACCESS_DENIED', 403);
    await respond(`${MOSQUES}/m-2/status`, error.body, error.options);
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('mu-status-message')).not.toBeNull();
  });
});
