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

  it('maakt de moskee aan en gaat naar haar gegevens', async () => {
    const { fixture, el, input } = await render();
    typeInto(input('name'), 'Voorbeeldmoskee');
    typeInto(input('city'), 'Utrecht');
    await fixture.whenStable();
    expect(input('slug').value).toBe('voorbeeldmoskee');

    buttonByText(el, 'Moskee toevoegen').click();
    const created = { ...MOSQUE, id: 'm-2', slug: 'voorbeeldmoskee', name: 'Voorbeeldmoskee' };
    const request = await respond((r) => r.url === MOSQUES && r.method === 'POST', created, {
      status: 201,
      statusText: 'Created',
    });
    expect(request.request.body).toEqual({
      slug: 'voorbeeldmoskee',
      details: expect.objectContaining({ name: 'Voorbeeldmoskee', city: 'Utrecht' }),
    });
    await respond((r) => r.url === MOSQUES && r.method === 'GET', [MOSQUE, created]);
    await fixture.whenStable();
    expect(router.navigate).toHaveBeenCalledWith(['/m', 'm-2', 'gegevens']);
    expect(fixture.componentInstance.hasUnsavedChanges()).toBe(false);
  });

  it('houdt een zelf aangepaste slug vast en controleert het formaat', async () => {
    const { fixture, el, input } = await render();
    typeInto(input('slug'), 'Fout Formaat');
    typeInto(input('name'), 'Andere naam');
    await fixture.whenStable();
    expect(input('slug').value).toBe('Fout Formaat');

    buttonByText(el, 'Moskee toevoegen').click();
    await fixture.whenStable();
    expect(text(el)).toContain('Gebruik kleine letters, cijfers en streepjes');
    http().expectNone((r) => r.method === 'POST');
    expect(fixture.componentInstance.hasUnsavedChanges()).toBe(true);
  });

  it('meldt een slug die al bestaat', async () => {
    const { fixture, el, input } = await render();
    typeInto(input('name'), 'Noeroel Islam');
    buttonByText(el, 'Moskee toevoegen').click();
    const error = apiError('SLUG_ALREADY_TAKEN', 409);
    await respond((r) => r.url === MOSQUES && r.method === 'POST', error.body, error.options);
    await fixture.whenStable();
    expect(text(el)).toContain('Er bestaat al een moskee met deze slug.');
    expect(router.navigate).not.toHaveBeenCalled();
  });
});
