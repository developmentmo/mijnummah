import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';

import { routes } from '../../app.routes';
import { configureTesting, http, text } from '../../testing/harness';
import { PRIVACY_STATEMENT_IS_CONCEPT, PrivacyPage } from './privacy-page';

describe('PrivacyPage', () => {
  beforeEach(() => configureTesting());
  afterEach(() => http().verify());

  async function render() {
    const fixture = TestBed.createComponent(PrivacyPage);
    fixture.detectChanges();
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  it('is openbaar: de route heeft geen guard', () => {
    const route = routes.find((r) => r.path === 'privacy');
    expect(route).toBeDefined();
    expect(route?.canActivate).toBeUndefined();
  });

  it('toont de verklaring zonder iets bij de API op te vragen', async () => {
    const root = await render();
    expect(text(root.querySelector('h1'))).toBe('Privacyverklaring Mijn Ummah');
    const headings = Array.from(root.querySelectorAll('h2')).map((h) => text(h));
    expect(headings).toEqual([
      'Wie zijn wij',
      'Gebruik zonder account',
      'Welke gegevens we verwerken, en waarom',
      'Hoe lang we gegevens bewaren',
      'Met wie we gegevens delen',
      'Je rechten',
      'Beveiliging',
    ]);
    http().expectNone(() => true);
  });

  it('zegt dat het een concept is zolang de tekst niet is vastgesteld', async () => {
    const root = await render();
    expect(root.querySelector('.concept') !== null).toBe(PRIVACY_STATEMENT_IS_CONCEPT);
  });
});
