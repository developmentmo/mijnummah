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
  active: false,
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
    const request = await respond(`${MOSQUES}/m-1/active`, { ...MOSQUE, active: false });
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({ active: false });
    await respond(
      (r) => r.url === MOSQUES && r.method === 'GET',
      [{ ...MOSQUE, active: false }, OTHER],
    );
    await fixture.whenStable();
    expect(text(fixture.nativeElement)).not.toContain('Deactiveren');
  });

  it('doet niets als de bevestiging wordt geannuleerd', async () => {
    ask.mockResolvedValue(false);
    const fixture = await render();
    buttonByText(fixture.nativeElement, 'Activeren').click();
    await fixture.whenStable();
    http().expectNone((r) => r.method === 'PUT');
  });

  it('meldt een fout bij (de)activeren', async () => {
    ask.mockResolvedValue(true);
    const fixture = await render();
    buttonByText(fixture.nativeElement, 'Activeren').click();
    await fixture.whenStable();
    const error = apiError('MOSQUE_ACCESS_DENIED', 403);
    await respond(`${MOSQUES}/m-2/active`, error.body, error.options);
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('mu-status-message')).not.toBeNull();
  });
});
