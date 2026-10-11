import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';

import { TokenResponse } from '../../core/api/models';
import {
  USER,
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
import { LoginPage } from './login-page';

const tokens: TokenResponse = {
  tokenType: 'Bearer',
  accessToken: 'access',
  accessTokenExpiresAt: new Date(Date.now() + 900_000).toISOString(),
  refreshToken: 'refresh',
  refreshTokenExpiresAt: new Date(Date.now() + 86_400_000).toISOString(),
};

describe('LoginPage', () => {
  let router: ReturnType<typeof spyOnRouter>;

  beforeEach(() => {
    sessionStorage.clear();
    configureTesting();
    router = spyOnRouter();
  });

  afterEach(() => http().verify());

  async function render(terug?: string) {
    const fixture = TestBed.createComponent(LoginPage);
    if (terug) fixture.componentRef.setInput('terug', terug);
    await fixture.whenStable();
    return fixture;
  }

  async function submit(
    fixture: Awaited<ReturnType<typeof render>>,
    email: string,
    password: string,
  ) {
    const el: HTMLElement = fixture.nativeElement;
    typeInto(el.querySelector('input[type=email]')!, email);
    typeInto(el.querySelector('input[type=password]')!, password);
    buttonByText(el, 'Inloggen').click();
    await fixture.whenStable();
  }

  it('valideert de velden voordat er iets verstuurd wordt', async () => {
    const fixture = await render();
    buttonByText(fixture.nativeElement, 'Inloggen').click();
    await fixture.whenStable();
    expect(text(fixture.nativeElement)).toContain('Vul dit veld in.');
    http().expectNone(`${V1}/auth/login`);
  });

  it('toont een duidelijke melding bij een verkeerd wachtwoord', async () => {
    const fixture = await render();
    await submit(fixture, 'beheerder@example.org', 'verkeerd');
    const { body, options } = apiError('INVALID_CREDENTIALS', 401);
    await respond(`${V1}/auth/login`, body, options);
    await fixture.whenStable();
    expect(text(fixture.nativeElement.querySelector('[role=alert]'))).toContain(
      'Het e-mailadres of wachtwoord klopt niet.',
    );
    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });

  it('noemt de wachttijd bij te veel pogingen', async () => {
    const fixture = await render();
    await submit(fixture, 'beheerder@example.org', 'verkeerd');
    const { body } = apiError('TOO_MANY_LOGIN_ATTEMPTS', 429);
    await respond(`${V1}/auth/login`, body, {
      status: 429,
      statusText: 'Too Many',
      headers: { 'Retry-After': '300' },
    });
    await fixture.whenStable();
    expect(text(fixture.nativeElement)).toContain('over 5 minuten');
  });

  it('gaat na inloggen terug naar de gevraagde pagina', async () => {
    const fixture = await render('/m/m-1/agenda');
    await submit(fixture, ' beheerder@example.org ', 'goed-wachtwoord');
    const login = await respond(`${V1}/auth/login`, tokens);
    expect(login.request.body.email).toBe('beheerder@example.org');
    await respond(`${V1}/auth/me`, USER);
    await fixture.whenStable();
    expect(router.navigateByUrl).toHaveBeenCalledWith('/m/m-1/agenda');
  });

  it('negeert een extern terugkeeradres', async () => {
    const fixture = await render('https://evil.example.org');
    await submit(fixture, 'beheerder@example.org', 'goed-wachtwoord');
    await respond(`${V1}/auth/login`, tokens);
    await respond(`${V1}/auth/me`, USER);
    await fixture.whenStable();
    expect(router.navigateByUrl).toHaveBeenCalledWith('/');
  });
});
