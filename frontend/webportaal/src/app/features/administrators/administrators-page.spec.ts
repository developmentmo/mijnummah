import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { Administrator } from '../../core/api/models';
import { Confirm } from '../../shared/ui/confirm-dialog';
import {
  MOSQUE,
  V1,
  apiError,
  buttonByText,
  configureTesting,
  http,
  respond,
  selectMosque,
  spyOnRouter,
  text,
  typeInto,
} from '../../testing/harness';
import { AdministratorsPage } from './administrators-page';

const BASE = `${V1}/admin/mosques/${MOSQUE.id}/administrators`;
const ADMIN: Administrator = {
  id: 'ad-1',
  userId: 'u-2',
  email: 'bestuur@example.org',
  displayName: 'Ahmed',
  role: 'ADMIN',
  createdAt: '2026-01-01T00:00:00Z',
};

describe('AdministratorsPage', () => {
  const ask = vi.fn();

  beforeEach(async () => {
    ask.mockReset();
    sessionStorage.clear();
    configureTesting([{ provide: Confirm, useValue: { ask } }]);
    spyOnRouter();
    await selectMosque();
  });

  afterEach(() => http().verify());

  async function render() {
    const fixture = TestBed.createComponent(AdministratorsPage);
    await fixture.whenStable();
    await respond(BASE, [ADMIN]);
    await fixture.whenStable();
    return fixture;
  }

  it('voegt een bestaand account toe met een rol', async () => {
    const fixture = await render();
    typeInto(fixture.nativeElement.querySelector('input[type=email]'), 'nieuw@example.org');
    buttonByText(fixture.nativeElement, 'Toevoegen').click();
    const add = await respond(BASE, {
      ...ADMIN,
      id: 'ad-2',
      email: 'nieuw@example.org',
      displayName: 'Nieuw',
      role: 'EDITOR',
    });
    expect(add.request.body).toEqual({ email: 'nieuw@example.org', role: 'EDITOR' });
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelectorAll('tbody tr').length).toBe(2);
  });

  it('legt uit dat het account eerst moet bestaan', async () => {
    const fixture = await render();
    typeInto(fixture.nativeElement.querySelector('input[type=email]'), 'onbekend@example.org');
    buttonByText(fixture.nativeElement, 'Toevoegen').click();
    const { body, options } = apiError('USER_NOT_FOUND', 404);
    await respond(BASE, body, options);
    await fixture.whenStable();
    expect(text(fixture.nativeElement)).toContain(
      'Een platformbeheerder kan het account aanmaken.',
    );
  });

  it('trekt rechten pas in na bevestiging en meldt de laatste beheerder', async () => {
    ask.mockResolvedValue(true);
    const fixture = await render();
    buttonByText(fixture.nativeElement, 'Intrekken').click();
    await fixture.whenStable();
    expect(ask).toHaveBeenCalledWith(expect.objectContaining({ destructive: true }));
    const { body, options } = apiError('LAST_ADMIN', 422);
    await respond(`${BASE}/ad-1`, body, options);
    await respond(BASE, [ADMIN]);
    await fixture.whenStable();
    expect(text(fixture.nativeElement)).toContain(
      'Er moet altijd minstens één Beheerder overblijven.',
    );
  });
});
