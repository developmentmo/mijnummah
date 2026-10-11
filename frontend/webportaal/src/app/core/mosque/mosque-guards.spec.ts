import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  RouterStateSnapshot,
  UrlTree,
  convertToParamMap,
} from '@angular/router';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { AuthService } from '../auth/auth.service';
import { MOSQUE, V1, configureTesting, respond, selectMosque } from '../../testing/harness';
import { MosqueContext } from './mosque-context';
import { mosqueAdminGuard, mosqueGuard } from './mosque-guards';

const routeFor = (mosqueId: string) =>
  ({ paramMap: convertToParamMap({ mosqueId }) }) as unknown as ActivatedRouteSnapshot;
const state = {} as RouterStateSnapshot;

describe('moskee-guards', () => {
  beforeEach(() => {
    sessionStorage.clear();
    configureTesting();
  });

  it('kiest de moskee uit de route als de beheerder er toegang toe heeft', async () => {
    const result = TestBed.runInInjectionContext(() => mosqueGuard(routeFor(MOSQUE.id), state));
    await respond(`${V1}/admin/mosques`, [MOSQUE]);
    expect(await result).toBe(true);
    expect(TestBed.inject(MosqueContext).current()?.name).toBe('Noeroel Islam');
  });

  it('stuurt terug naar het moskeeoverzicht bij een moskee zonder toegang', async () => {
    const result = TestBed.runInInjectionContext(() => mosqueGuard(routeFor('andere'), state));
    await respond(`${V1}/admin/mosques`, [MOSQUE]);
    expect(((await result) as UrlTree).toString()).toBe('/moskeeen?melding=geen-toegang');
  });

  it('laat alleen de rol Beheerder bij beheerdersschermen', async () => {
    await selectMosque({ ...MOSQUE, myRole: 'EDITOR' });
    const denied = TestBed.runInInjectionContext(() =>
      mosqueAdminGuard(routeFor(MOSQUE.id), state),
    );
    expect((denied as UrlTree).toString()).toBe(`/m/${MOSQUE.id}`);
  });

  it('geeft een platformbeheerder zonder eigen rol alle rechten', async () => {
    vi.spyOn(TestBed.inject(AuthService), 'isPlatformAdmin').mockReturnValue(true);
    await selectMosque({ ...MOSQUE, myRole: null });
    expect(TestBed.inject(MosqueContext).role()).toBe('ADMIN');
    expect(TestBed.runInInjectionContext(() => mosqueAdminGuard(routeFor(MOSQUE.id), state))).toBe(
      true,
    );
  });
});
