import { HttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';

import { TokenResponse } from '../api/models';
import {
  USER,
  V1,
  apiError,
  configureTesting,
  http,
  respond,
  settle,
  spyOnRouter,
} from '../../testing/harness';
import { AuthService } from './auth.service';

const tokens = (n: number): TokenResponse => ({
  tokenType: 'Bearer',
  accessToken: `access-${n}`,
  accessTokenExpiresAt: new Date(Date.now() + 15 * 60_000).toISOString(),
  refreshToken: `refresh-${n}`,
  refreshTokenExpiresAt: new Date(Date.now() + 86_400_000).toISOString(),
});

describe('authInterceptor', () => {
  let client: HttpClient;
  let router: ReturnType<typeof spyOnRouter>;

  beforeEach(async () => {
    sessionStorage.clear();
    configureTesting();
    client = TestBed.inject(HttpClient);
    router = spyOnRouter();
    const login = TestBed.inject(AuthService).login('a@example.org', 'b');
    await respond(`${V1}/auth/login`, tokens(1));
    await respond(`${V1}/auth/me`, USER);
    await login;
  });

  afterEach(() => http().verify());

  it('zet het access token op API-verzoeken', async () => {
    const result = firstValueFrom(client.get(`${V1}/admin/mosques`));
    const request = await respond(`${V1}/admin/mosques`, []);
    expect(request.request.headers.get('Authorization')).toBe('Bearer access-1');
    await result;
  });

  it('laat verzoeken naar andere adressen ongemoeid', async () => {
    const result = firstValueFrom(client.get('https://elders.example.org/data'));
    const request = await respond('https://elders.example.org/data', {});
    expect(request.request.headers.has('Authorization')).toBe(false);
    await result;
  });

  it('ververst na een 401 en probeert het verzoek één keer opnieuw', async () => {
    const result = firstValueFrom(client.get<string[]>(`${V1}/admin/mosques`));
    const { body, options } = apiError('UNAUTHORIZED', 401);
    await respond(`${V1}/admin/mosques`, body, options);
    await respond(`${V1}/auth/refresh`, tokens(2));
    const retry = await respond(`${V1}/admin/mosques`, ['ok']);
    expect(retry.request.headers.get('Authorization')).toBe('Bearer access-2');
    expect(await result).toEqual(['ok']);
  });

  it('stuurt naar de inlogpagina als verversen ook mislukt', async () => {
    const result = firstValueFrom(client.get(`${V1}/admin/mosques`)).catch((e: unknown) => e);
    const unauthorized = apiError('UNAUTHORIZED', 401);
    await respond(`${V1}/admin/mosques`, unauthorized.body, unauthorized.options);
    const invalid = apiError('INVALID_REFRESH_TOKEN', 401);
    await respond(`${V1}/auth/refresh`, invalid.body, invalid.options);
    expect(await result).toMatchObject({ status: 401 });
    await settle();
    expect(TestBed.inject(AuthService).isSignedIn()).toBe(false);
    expect(TestBed.inject(AuthService).lastEndReason()).toBe('expired');
    expect(router.navigate).toHaveBeenCalledWith(['/inloggen'], expect.anything());
  });

  it('ververst niet bij een 403', async () => {
    const result = firstValueFrom(client.get(`${V1}/admin/mosques/x/administrators`)).catch(
      (e: unknown) => e,
    );
    const { body, options } = apiError('MOSQUE_ACCESS_DENIED', 403);
    await respond(`${V1}/admin/mosques/x/administrators`, body, options);
    expect(await result).toMatchObject({ status: 403 });
    http().expectNone(`${V1}/auth/refresh`);
  });
});
