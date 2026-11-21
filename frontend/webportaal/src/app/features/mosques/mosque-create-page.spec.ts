import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';

import {
  MOSQUE,
  V1,
  apiError,
  buttonByText,
  configureTesting,
  http,
  respond,
  spyOnRouter,
  text,
  typeInto,
} from '../../testing/harness';
import { MosqueCreatePage, slugFrom } from './mosque-create-page';

const MOSQUES = `${V1}/admin/mosques`;

describe('MosqueCreatePage', () => {
  let router: ReturnType<typeof spyOnRouter>;

  beforeEach(() => {
    sessionStorage.clear();
    configureTesting();
    router = spyOnRouter();
  });

  afterEach(() => http().verify());

  async function render() {
    const fixture = TestBed.createComponent(MosqueCreatePage);
    await fixture.whenStable();
    const el: HTMLElement = fixture.nativeElement;
    const input = (name: string) =>
      el.querySelector<HTMLInputElement>(`input[formcontrolname=${name}]`)!;
    return { fixture, el, input };
  }

  it('stelt de slug voor uit de naam', () => {
    expect(slugFrom('Moskee Al-Fath  Utrecht')).toBe('moskee-al-fath-utrecht');
    expect(slugFrom('Mesquita São João!')).toBe('mesquita-sao-joao');
    expect(slugFrom('a'.repeat(59) + ' b')).toBe('a'.repeat(59));
  });

  function created(invited: boolean | null) {
    return {
      mosque: { ...MOSQUE, id: 'm-2', slug: 'voorbeeldmoskee', name: 'Voorbeeldmoskee' },
      settings: { registrationsEnabled: true, pushEnabled: true },
      firstAdministrator: invited === null ? null : { userId: 'u-9', invited },
    };
  }

  it('registreert de moskee met haar eerste beheerder en laat de slug aan de backend', async () => {
    const { fixture, el, input } = await render();
    typeInto(input('name'), 'Voorbeeldmoskee');
    typeInto(input('city'), 'Utrecht');
    typeInto(input('adminName'), 'Bestuur');
    typeInto(input('adminEmail'), 'bestuur@example.org');
    await fixture.whenStable();
    expect(input('slug').value).toBe('voorbeeldmoskee');

    buttonByText(el, 'Moskee registreren').click();
    const request = await respond((r) => r.url === MOSQUES && r.method === 'POST', created(true), {
      status: 201,
      statusText: 'Created',
    });
    expect(request.request.body).toEqual({
      slug: null,
      details: expect.objectContaining({ name: 'Voorbeeldmoskee', city: 'Utrecht' }),
      firstAdministrator: { email: 'bestuur@example.org', displayName: 'Bestuur' },
      settings: { registrationsEnabled: true, pushEnabled: true },
    });
    await respond((r) => r.url === MOSQUES && r.method === 'GET', [MOSQUE, created(true).mosque]);
    await fixture.whenStable();
    expect(router.navigate).toHaveBeenCalledWith(['/m', 'm-2', 'gegevens']);
    expect(fixture.componentInstance.hasUnsavedChanges()).toBe(false);
  });

  it('vraagt de gegevens van de beheerder, tenzij die er later bij komt', async () => {
    const { fixture, el, input } = await render();
    typeInto(input('name'), 'Voorbeeldmoskee');
    buttonByText(el, 'Moskee registreren').click();
    await fixture.whenStable();
    http().expectNone((r) => r.method === 'POST');
    expect(text(el)).toContain('Vul dit veld in');

    el.querySelector<HTMLButtonElement>('mat-slide-toggle button')!.click();
    await fixture.whenStable();
    buttonByText(el, 'Moskee registreren').click();
    const request = await respond((r) => r.url === MOSQUES && r.method === 'POST', created(null), {
      status: 201,
      statusText: 'Created',
    });
    expect(request.request.body.firstAdministrator).toBeNull();
    await respond((r) => r.url === MOSQUES && r.method === 'GET', [MOSQUE]);
  });

  it('stuurt een zelf gekozen slug mee en controleert het formaat', async () => {
    const { fixture, el, input } = await render();
    typeInto(input('slug'), 'Fout Formaat');
    typeInto(input('name'), 'Andere naam');
    await fixture.whenStable();
    expect(input('slug').value).toBe('Fout Formaat');

    buttonByText(el, 'Moskee registreren').click();
    await fixture.whenStable();
    expect(text(el)).toContain('Gebruik kleine letters, cijfers en streepjes');
    http().expectNone((r) => r.method === 'POST');
    expect(fixture.componentInstance.hasUnsavedChanges()).toBe(true);
  });

  it('meldt een slug die al bestaat en een geblokkeerde beheerder', async () => {
    const { fixture, el, input } = await render();
    typeInto(input('name'), 'Noeroel Islam');
    typeInto(input('slug'), 'noeroel-islam');
    typeInto(input('adminName'), 'Bestuur');
    typeInto(input('adminEmail'), 'bestuur@example.org');
    buttonByText(el, 'Moskee registreren').click();
    let error = apiError('SLUG_ALREADY_TAKEN', 409);
    await respond((r) => r.url === MOSQUES && r.method === 'POST', error.body, error.options);
    await fixture.whenStable();
    expect(text(el)).toContain('Er bestaat al een moskee met dit adres in de app.');

    buttonByText(el, 'Moskee registreren').click();
    error = apiError('ACCOUNT_BLOCKED', 403);
    await respond((r) => r.url === MOSQUES && r.method === 'POST', error.body, error.options);
    await fixture.whenStable();
    expect(text(el)).toContain('Het account van de eerste beheerder is geblokkeerd.');
    expect(router.navigate).not.toHaveBeenCalled();
  });
});
