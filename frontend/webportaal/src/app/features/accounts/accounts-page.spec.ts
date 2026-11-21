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

  function fill(el: HTMLElement) {
    typeInto(el.querySelector('input[formcontrolname=displayName]')!, 'Nieuwe beheerder');
    typeInto(el.querySelector('input[formcontrolname=email]')!, 'nieuw@example.org');
  }

  it('maakt standaard een account met een uitnodiging per e-mail', async () => {
    const fixture = await render();
    const el: HTMLElement = fixture.nativeElement;
    fill(el);
    expect(el.querySelector('input[type=password]')).toBeNull();
    buttonByText(el, 'Account aanmaken').click();
    const request = await respond((r) => r.url === USERS && r.method === 'POST', {
      ...USER,
      id: 'u-2',
      lastLoginAt: null,
    });
    expect(request.request.body).toEqual({
      email: 'nieuw@example.org',
      displayName: 'Nieuwe beheerder',
      password: null,
      platformRole: 'USER',
    });
    await respond((r) => r.url === USERS && r.method === 'GET', page([USER]));
  });

  it('eist zonder uitnodiging een wachtwoord van minstens 12 tekens', async () => {
    const fixture = await render();
    const el: HTMLElement = fixture.nativeElement;
    el.querySelector<HTMLButtonElement>('mat-slide-toggle button')!.click();
    await fixture.whenStable();
    fill(el);
    typeInto(el.querySelector('input[type=password]')!, 'kort');
    buttonByText(el, 'Account aanmaken').click();
    await fixture.whenStable();
    expect(text(el)).toContain('Minimaal 12 tekens.');
    http().expectNone((r) => r.method === 'POST');
  });

  it('zoekt op naam of e-mailadres', async () => {
    const fixture = await render();
    typeInto(fixture.nativeElement.querySelector('input[type=search]')!, 'fatima');
    await new Promise((resolve) => setTimeout(resolve, 350));
    const request = await respond((r) => r.url === USERS && r.method === 'GET', page([USER]));
    expect(request.request.params.get('q')).toBe('fatima');
  });

  it('deactiveert een account pas na bevestiging', async () => {
    ask.mockResolvedValue(true);
    const fixture = await render();
    buttonByText(fixture.nativeElement, 'Deactiveren').click();
    await fixture.whenStable();
    expect(ask).toHaveBeenCalledWith(
      expect.objectContaining({ title: 'Account deactiveren?', destructive: true }),
    );
    await respond(`${USERS}/u-1/block`, { ...USER, status: 'BLOCKED' });
    await fixture.whenStable();
    expect(text(fixture.nativeElement.querySelector('tbody'))).toContain('Gedeactiveerd');
  });

  it('stuurt na bevestiging een code voor een nieuw wachtwoord', async () => {
    ask.mockResolvedValue(true);
    const fixture = await render();
    buttonByText(fixture.nativeElement, 'Code sturen').click();
    await fixture.whenStable();
    const request = await respond(`${USERS}/u-1/password-code`, null, {
      status: 204,
      statusText: 'No Content',
    });
    expect(request.request.method).toBe('POST');
  });
});
