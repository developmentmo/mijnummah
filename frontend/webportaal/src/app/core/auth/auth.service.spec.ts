import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';

import { TokenResponse } from '../api/models';
import { USER, V1, apiError, configureTesting, http, respond, settle } from '../../testing/harness';
import { AuthService } from './auth.service';
import { RefreshTokenStore } from './refresh-token-store';

function tokens(n: number, expiresInMs = 15 * 60_000): TokenResponse {
  return {
    tokenType: 'Bearer',
    accessToken: `access-${n}`,
    accessTokenExpiresAt: new Date(Date.now() + expiresInMs).toISOString(),
    refreshToken: `refresh-${n}`,
    refreshTokenExpiresAt: new Date(Date.now() + 86_400_000).toISOString(),
  };
}

describe('AuthService', () => {
  let auth: AuthService;
  let store: RefreshTokenStore;

  beforeEach(() => {
    sessionStorage.clear();
    configureTesting();
    auth = TestBed.inject(AuthService);
    store = TestBed.inject(RefreshTokenStore);
  });

  afterEach(() => http().verify());

  it('logt in, bewaart alleen het refresh token en haalt het account op', async () => {
    const login = auth.login('beheerder@example.org', 'geheim-wachtwoord');
    const loginRequest = await respond(`${V1}/auth/login`, tokens(1));
    expect(loginRequest.request.body).toEqual({
      email: 'beheerder@example.org',
      password: 'geheim-wachtwoord',
    });
    const me = await respond(`${V1}/auth/me`, USER);
    expect(me.request.headers.get('Authorization')).toBe('Bearer access-1');
    await login;

    expect(auth.user()).toEqual(USER);
    expect(store.read()).toBe('refresh-1');
    expect(JSON.stringify(sessionStorage)).not.toContain('access-1');
  });

  it('geeft de foutcode door bij een mislukte inlog en bewaart niets', async () => {
    const login = auth.login('x@example.org', 'fout').catch((error: unknown) => error);
    const { body, options } = apiError('INVALID_CREDENTIALS', 401);
    await respond(`${V1}/auth/login`, body, options);
    expect(await login).toMatchObject({ code: 'INVALID_CREDENTIALS' });
    expect(auth.isSignedIn()).toBe(false);
    expect(store.read()).toBeNull();
  });

  it('herstelt een sessie na herladen met het opgeslagen refresh token', async () => {
    store.write('refresh-0');
    const restoring = auth.restore();
    const refresh = await respond(`${V1}/auth/refresh`, tokens(1));
    expect(refresh.request.body).toEqual({ refreshToken: 'refresh-0' });
    await respond(`${V1}/auth/me`, USER);
    await restoring;
    expect(auth.user()?.displayName).toBe('Fatima');
    expect(store.read()).toBe('refresh-1');
  });

  it('beëindigt de sessie als het refresh token niet meer geldig is', async () => {
    store.write('refresh-oud');
    const restoring = auth.restore();
    const { body, options } = apiError('INVALID_REFRESH_TOKEN', 401);
    await respond(`${V1}/auth/refresh`, body, options);
    await restoring;
    expect(auth.isSignedIn()).toBe(false);
    expect(auth.lastEndReason()).toBe('expired');
    expect(store.read()).toBeNull();
  });

  it('bewaart het refresh token als de server onbereikbaar is', async () => {
    store.write('refresh-0');
    const restoring = auth.restore().catch((error: unknown) => error);
    await settle();
    http().expectOne(`${V1}/auth/refresh`).error(new ProgressEvent('error'), { status: 0 });
    expect(await restoring).toMatchObject({ code: 'NETWORK' });
    expect(store.read()).toBe('refresh-0');
  });

  it('ververst maar één keer als meerdere verzoeken tegelijk een token nodig hebben', async () => {
    store.write('refresh-0');
    const first = auth.validAccessToken();
    const second = auth.validAccessToken();
    await settle();
    const requests = http().match(`${V1}/auth/refresh`);
    expect(requests).toHaveLength(1);
    requests[0].flush(tokens(1));
    expect(await first).toBe('access-1');
    expect(await second).toBe('access-1');
  });

  it('ververst het token vlak voordat het verloopt', async () => {
    const login = auth.login('a@example.org', 'b');
    await respond(`${V1}/auth/login`, tokens(1, 10_000));
    await respond(`${V1}/auth/me`, USER);
    await login;
    const token = auth.validAccessToken();
    await respond(`${V1}/auth/refresh`, tokens(2));
    expect(await token).toBe('access-2');
  });

  it('logt lokaal uit, ook als de server niet reageert', async () => {
    store.write('refresh-9');
    const logout = auth.logout();
    expect(store.read()).toBeNull();
    await settle();
    http().expectOne(`${V1}/auth/logout`).error(new ProgressEvent('error'), { status: 0 });
    await logout;
    expect(auth.isSignedIn()).toBe(false);
    expect(auth.lastEndReason()).toBe('logout');
  });
});
