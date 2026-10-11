import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree } from '@angular/router';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { configureTesting } from '../../testing/harness';
import { AuthService } from './auth.service';
import { authGuard, guestGuard, platformAdminGuard, safeReturnUrl } from './guards';

const route = {} as ActivatedRouteSnapshot;
const state = (url: string) => ({ url }) as RouterStateSnapshot;

describe('route guards', () => {
  let auth: AuthService;

  beforeEach(() => {
    sessionStorage.clear();
    configureTesting();
    auth = TestBed.inject(AuthService);
  });

  function signIn(platformRole: 'USER' | 'PLATFORM_ADMIN' = 'USER') {
    vi.spyOn(auth, 'isSignedIn').mockReturnValue(true);
    vi.spyOn(auth, 'isPlatformAdmin').mockReturnValue(platformRole === 'PLATFORM_ADMIN');
  }

  it('stuurt bezoekers zonder sessie naar inloggen, met de gevraagde pagina', async () => {
    const result = (await TestBed.runInInjectionContext(() =>
      authGuard(route, state('/m/1/agenda?status=DRAFT')),
    )) as UrlTree;
    expect(result.toString()).toBe('/inloggen?terug=%2Fm%2F1%2Fagenda%3Fstatus%3DDRAFT');
  });

  it('laat ingelogde beheerders door', async () => {
    signIn();
    expect(await TestBed.runInInjectionContext(() => authGuard(route, state('/')))).toBe(true);
  });

  it('stuurt ingelogde beheerders weg van de inlogpagina', async () => {
    signIn();
    const result = (await TestBed.runInInjectionContext(() =>
      guestGuard(route, state('/inloggen')),
    )) as UrlTree;
    expect(result.toString()).toBe('/');
  });

  it('laat alleen platformbeheerders bij accountbeheer', () => {
    signIn('USER');
    expect(
      TestBed.runInInjectionContext(() => platformAdminGuard(route, state('/accounts'))),
    ).toBeInstanceOf(UrlTree);
    signIn('PLATFORM_ADMIN');
    expect(TestBed.runInInjectionContext(() => platformAdminGuard(route, state('/accounts')))).toBe(
      true,
    );
  });

  it('accepteert alleen interne terugkeeradressen', () => {
    expect(safeReturnUrl('/m/1/agenda')).toBe('/m/1/agenda');
    expect(safeReturnUrl('//evil.example.org')).toBe('/');
    expect(safeReturnUrl('https://evil.example.org')).toBe('/');
    expect(safeReturnUrl('/inloggen')).toBe('/');
    expect(safeReturnUrl(undefined)).toBe('/');
  });
});
