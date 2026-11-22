import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { PushNotification } from '../../core/api/models';
import { Confirm } from '../../shared/ui/confirm-dialog';
import {
  AUDIENCE,
  MOSQUE,
  V1,
  aPushNotification,
  buttonByText,
  configureTesting,
  http,
  page,
  respond,
  selectMosque,
  settle,
  text,
  typeInto,
} from '../../testing/harness';
import { PushPage } from './push-page';

const BASE = `${V1}/admin/mosques/${MOSQUE.id}/notifications`;

const MESSAGE: PushNotification = aPushNotification({
  id: 'p-2',
  contentType: 'MESSAGE',
  contentId: null,
  audience: 'EVENTS',
  title: 'Moskee dicht',
  body: 'Vanavond geen isha in de moskee.',
});

describe('PushPage', () => {
  const ask = vi.fn();

  beforeEach(() => {
    ask.mockReset();
    sessionStorage.clear();
    configureTesting([{ provide: Confirm, useValue: { ask } }]);
  });

  afterEach(() => http().verify());

  async function render(role: 'ADMIN' | 'EDITOR' = 'ADMIN', history = [MESSAGE]) {
    await selectMosque({ ...MOSQUE, myRole: role });
    const fixture = TestBed.createComponent(PushPage);
    await fixture.whenStable();
    await settle();
    if (role === 'ADMIN') {
      const audience = http().expectOne((r) => r.url === `${BASE}/audience`);
      expect(audience.request.params.get('audience')).toBe('ALL_FOLLOWERS');
      audience.flush(AUDIENCE);
    }
    http()
      .expectOne((r) => r.url === BASE && r.method === 'GET')
      .flush(page(history));
    await settle();
    await fixture.whenStable();
    return fixture;
  }

  it('toont verstuurde meldingen met doelgroep en verzendstatus', async () => {
    const fixture = await render();
    const content = text(fixture.nativeElement);
    expect(content).toContain('Eigen bericht');
    expect(content).toContain('Volgers die meldingen over activiteiten ontvangen');
    expect(content).toContain('Moskee dicht');
    expect(content).toContain('14 van 15 afgeleverd, 1 mislukt');
    expect(content).toContain('Bereikt nu 12 volgers op 15 apparaten.');
  });

  it('verstuurt een eigen bericht pas na bevestiging', async () => {
    ask.mockResolvedValue(true);
    const fixture = await render('ADMIN', []);
    const root: HTMLElement = fixture.nativeElement;
    typeInto(root.querySelector('input[formcontrolname=title]')!, '  Moskee dicht ');
    typeInto(root.querySelector('textarea[formcontrolname=body]')!, 'Vanavond geen isha.');
    await fixture.whenStable();
    buttonByText(root, 'Pushmelding versturen').click();
    await fixture.whenStable();

    expect(ask).toHaveBeenCalledWith(
      expect.objectContaining({ message: expect.stringContaining('12 volgers') }),
    );
    const sent = await respond((r) => r.url === BASE && r.method === 'POST', MESSAGE);
    expect(sent.request.body).toEqual({
      title: 'Moskee dicht',
      body: 'Vanavond geen isha.',
      audience: 'ALL_FOLLOWERS',
    });
    await respond((r) => r.url === BASE && r.method === 'GET', page([MESSAGE]));
    await fixture.whenStable();
    expect(text(root)).toContain('14 van 15 afgeleverd');
  });

  it('verstuurt niets zonder bevestiging', async () => {
    ask.mockResolvedValue(false);
    const fixture = await render('ADMIN', []);
    const root: HTMLElement = fixture.nativeElement;
    typeInto(root.querySelector('input[formcontrolname=title]')!, 'Titel');
    typeInto(root.querySelector('textarea[formcontrolname=body]')!, 'Tekst');
    await fixture.whenStable();
    buttonByText(root, 'Pushmelding versturen').click();
    await settle();
    http().expectNone((r) => r.method === 'POST');
  });

  it('laat een redacteur alleen de geschiedenis zien', async () => {
    const fixture = await render('EDITOR');
    const content = text(fixture.nativeElement);
    expect(content).toContain('Alleen een beheerder van de moskee kan een pushmelding versturen.');
    expect(fixture.nativeElement.querySelector('form')).toBeNull();
    expect(content).toContain('Moskee dicht');
  });
});
