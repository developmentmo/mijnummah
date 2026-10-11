import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { Confirm } from '../../shared/ui/confirm-dialog';
import {
  USER,
  V1,
  buttonByText,
  configureTesting,
  http,
  page,
  respond,
  text,
  typeInto,
} from '../../testing/harness';
import { AccountsPage } from './accounts-page';

const USERS = `${V1}/admin/users`;

describe('AccountsPage', () => {
  const ask = vi.fn();

  beforeEach(() => {
    ask.mockReset();
    sessionStorage.clear();
    configureTesting([{ provide: Confirm, useValue: { ask } }]);
  });

  afterEach(() => http().verify());

  async function render() {
    const fixture = TestBed.createComponent(AccountsPage);
    await fixture.whenStable();
    await respond((r) => r.url === USERS && r.method === 'GET', page([USER]));
    await fixture.whenStable();
    return fixture;
  }

  it('eist een wachtwoord van minstens 12 tekens', async () => {
    const fixture = await render();
    const el: HTMLElement = fixture.nativeElement;
    typeInto(el.querySelector('input[formcontrolname=displayName]')!, 'Nieuwe beheerder');
    typeInto(el.querySelector('input[type=email]')!, 'nieuw@example.org');
    typeInto(el.querySelector('input[type=password]')!, 'kort');
    buttonByText(el, 'Account aanmaken').click();
    await fixture.whenStable();
    expect(text(el)).toContain('Minimaal 12 tekens.');
    http().expectNone((r) => r.method === 'POST');
  });

  it('blokkeert een account pas na bevestiging', async () => {
    ask.mockResolvedValue(true);
    const fixture = await render();
    buttonByText(fixture.nativeElement, 'Blokkeren').click();
    await fixture.whenStable();
    expect(ask).toHaveBeenCalledWith(
      expect.objectContaining({ title: 'Account blokkeren?', destructive: true }),
    );
    await respond(`${USERS}/u-1/block`, { ...USER, status: 'BLOCKED' });
    await fixture.whenStable();
    expect(text(fixture.nativeElement.querySelector('tbody'))).toContain('Geblokkeerd');
  });
});
