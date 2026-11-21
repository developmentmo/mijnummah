import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { AdminMosque } from '../../core/api/models';
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
} from '../../testing/harness';
import { MosquesAdminPage } from './mosques-admin-page';

const MOSQUES = `${V1}/admin/mosques`;
const OTHER: AdminMosque = {
  ...MOSQUE,
  id: 'm-2',
  slug: 'voorbeeldmoskee',
  name: 'Voorbeeldmoskee',
  status: 'INACTIVE',
  myRole: null,
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
    await respond((r) => r.url === MOSQUES && r.method === 'GET', [MOSQUE, OTHER]);
    await fixture.whenStable();
    return fixture;
  }

  it('toont alle moskeeën met hun status', async () => {
    const fixture = await render();
    const el: HTMLElement = fixture.nativeElement;
    expect(text(el)).toContain('Noeroel Islam');
    expect(text(el)).toContain('voorbeeldmoskee');
    expect(text(el)).toContain('Actief');
    expect(text(el)).toContain('Inactief');
    expect(el.querySelectorAll('a[href="/m/m-2"]')).toHaveLength(1);
  });

  it('deactiveert pas na bevestiging en ververst de lijst', async () => {
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
    await respond(
      (r) => r.url === MOSQUES && r.method === 'GET',
      [{ ...MOSQUE, status: 'INACTIVE' }, OTHER],
    );
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
    const blocked = { ...MOSQUE, status: 'BLOCKED' as const };
    const request = await respond(`${MOSQUES}/m-1/status`, blocked);
    expect(request.request.body).toEqual({ status: 'BLOCKED' });
    await respond((r) => r.url === MOSQUES && r.method === 'GET', [blocked, OTHER]);
    await fixture.whenStable();
    expect(text(el)).toContain('Geblokkeerd');

    buttonByText(el, 'Blokkade opheffen').click();
    await fixture.whenStable();
    expect(ask).toHaveBeenLastCalledWith(
      expect.objectContaining({ title: 'Blokkade van Noeroel Islam opheffen?' }),
    );
    const lifted = await respond(`${MOSQUES}/m-1/status`, MOSQUE);
    expect(lifted.request.body).toEqual({ status: 'ACTIVE' });
    await respond((r) => r.url === MOSQUES && r.method === 'GET', [MOSQUE, OTHER]);
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
