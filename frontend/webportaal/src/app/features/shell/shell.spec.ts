import { TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { AuthService } from '../../core/auth/auth.service';
import { MOSQUE, USER, configureTesting, selectMosque, text } from '../../testing/harness';
import { Shell } from './shell';

describe('Shell', () => {
  beforeEach(() => {
    sessionStorage.clear();
    configureTesting();
    const auth = TestBed.inject(AuthService);
    vi.spyOn(auth, 'user').mockReturnValue(USER);
  });

  async function navLabels() {
    const fixture = TestBed.createComponent(Shell);
    await fixture.whenStable();
    return Array.from(
      fixture.nativeElement.querySelectorAll('nav a') as NodeListOf<HTMLElement>,
    ).map(text);
  }

  it('toont beheerschermen alleen voor de rol Beheerder', async () => {
    await selectMosque({ ...MOSQUE, myRole: 'ADMIN' });
    expect(await navLabels()).toEqual([
      'Overzicht',
      'Agenda',
      'Lezingen en video’s',
      'Aankondigingen',
      'Moskeegegevens',
      'Beheerders',
    ]);
  });

  it('toont een redacteur alleen de content', async () => {
    await selectMosque({ ...MOSQUE, myRole: 'EDITOR' });
    expect(await navLabels()).toEqual([
      'Overzicht',
      'Agenda',
      'Lezingen en video’s',
      'Aankondigingen',
    ]);
  });

  it('heeft een skiplink naar de inhoud', async () => {
    const fixture = TestBed.createComponent(Shell);
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('a.mu-skip-link').getAttribute('href')).toBe(
      '#inhoud',
    );
    expect(fixture.nativeElement.querySelector('main#inhoud')).not.toBeNull();
  });
});
