import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { PushAudience, PushNotification } from '../../core/api/models';
import { Confirm } from '../../shared/ui/confirm-dialog';
import {
  AUDIENCE,
  MOSQUE,
  V1,
  aPushNotification,
  apiError,
  buttonByText,
  configureTesting,
  http,
  respond,
  respondPushPanel,
  selectMosque,
  text,
  typeInto,
} from '../../testing/harness';
import { PushPanel } from './push-panel';

const SEND = `${V1}/admin/mosques/${MOSQUE.id}/announcements/a-1/push`;

describe('PushPanel', () => {
  const ask = vi.fn();

  beforeEach(() => {
    ask.mockReset();
    sessionStorage.clear();
    configureTesting([{ provide: Confirm, useValue: { ask } }]);
  });

  afterEach(() => http().verify());

  async function render(
    options: {
      eligible?: boolean;
      blocked?: boolean;
      audience?: PushAudience;
      sent?: PushNotification | null;
      role?: 'ADMIN' | 'EDITOR';
    } = {},
  ) {
    await selectMosque({ ...MOSQUE, myRole: options.role ?? 'ADMIN' });
    const fixture = TestBed.createComponent(PushPanel);
    fixture.componentRef.setInput('contentType', 'ANNOUNCEMENT');
    fixture.componentRef.setInput('contentId', 'a-1');
    fixture.componentRef.setInput('eligible', options.eligible ?? true);
    fixture.componentRef.setInput('ineligibleReason', 'Publiceer de aankondiging eerst.');
    fixture.componentRef.setInput('blocked', options.blocked ?? false);
    fixture.componentRef.setInput('defaultTitleHint', 'de titel van de aankondiging');
    fixture.componentRef.setInput('defaultBodyHint', 'de tekst van de aankondiging');
    await fixture.whenStable();
    await respondPushPanel(options.audience ?? AUDIENCE, options.sent ?? null);
    await fixture.whenStable();
    return fixture;
  }

  it('zegt dat publiceren geen pushmelding verstuurt en toont het bereik', async () => {
    const fixture = await render();
    const panel = text(fixture.nativeElement);
    expect(panel).toContain('Publiceren verstuurt nooit een pushmelding');
    expect(panel).toContain('Deze melding gaat nu naar 12 volgers op 15 apparaten.');
    expect(buttonByText(fixture.nativeElement, 'Pushmelding versturen').disabled).toBe(false);
  });

  it('verstuurt pas na bevestiging, met eigen titel en lege tekst als standaard', async () => {
    ask.mockResolvedValue(true);
    const fixture = await render();
    typeInto(fixture.nativeElement.querySelector('input'), '  Iftar vanavond ');
    await fixture.whenStable();
    buttonByText(fixture.nativeElement, 'Pushmelding versturen').click();
    await fixture.whenStable();

    expect(ask).toHaveBeenCalledWith(
      expect.objectContaining({
        title: 'Pushmelding versturen?',
        message: expect.stringContaining('maar één keer per aankondiging'),
      }),
    );
    const request = await respond(
      SEND,
      aPushNotification({ status: 'SENDING', deliveredCount: 0 }),
    );
    expect(request.request.body).toEqual({ title: 'Iftar vanavond', body: null });
    await fixture.whenStable();
    expect(text(fixture.nativeElement)).toContain('Wordt verstuurd naar 15 apparaten');
    expect(buttonByText(fixture.nativeElement, 'Status vernieuwen')).toBeTruthy();
  });

  it('verstuurt niets als de bevestiging wordt afgewezen', async () => {
    ask.mockResolvedValue(false);
    const fixture = await render();
    buttonByText(fixture.nativeElement, 'Pushmelding versturen').click();
    await fixture.whenStable();
    http().expectNone(SEND);
  });

  it('toont het resultaat van een eerder verstuurde melding en geen verstuurknop', async () => {
    const fixture = await render({ sent: aPushNotification() });
    const panel = text(fixture.nativeElement);
    expect(panel).toContain('Iftar vrijdag');
    expect(panel).toContain('Afgeleverd op 14 apparaten, 1 niet bereikt.');
    expect(() => buttonByText(fixture.nativeElement, 'Pushmelding versturen')).toThrow();
  });

  it('gebruikt enkelvoud bij één apparaat', async () => {
    const fixture = await render({
      sent: aPushNotification({ recipientCount: 1, deliveredCount: 1, failedCount: 0 }),
    });
    expect(text(fixture.nativeElement)).toContain('Afgeleverd op 1 apparaat.');
  });

  it('legt uit dat pushmeldingen nog niet zijn ingesteld', async () => {
    const fixture = await render({ audience: { ...AUDIENCE, configured: false } });
    expect(text(fixture.nativeElement)).toContain('nog niet ingesteld');
    expect(() => buttonByText(fixture.nativeElement, 'Pushmelding versturen')).toThrow();
  });

  it('laat alleen een beheerder versturen', async () => {
    const fixture = await render({ role: 'EDITOR' });
    expect(text(fixture.nativeElement)).toContain('Alleen een beheerder van de moskee');
    expect(() => buttonByText(fixture.nativeElement, 'Pushmelding versturen')).toThrow();
  });

  it('legt uit waarom een item nog niet verstuurd kan worden', async () => {
    const fixture = await render({ eligible: false });
    expect(text(fixture.nativeElement)).toContain('Publiceer de aankondiging eerst.');
    expect(() => buttonByText(fixture.nativeElement, 'Pushmelding versturen')).toThrow();
  });

  it('blokkeert versturen bij niet-opgeslagen wijzigingen', async () => {
    const fixture = await render({ blocked: true });
    expect(buttonByText(fixture.nativeElement, 'Pushmelding versturen').disabled).toBe(true);
    expect(text(fixture.nativeElement)).toContain('Sla eerst je wijzigingen op.');
  });

  it('blokkeert versturen als niemand de melding zou ontvangen', async () => {
    const fixture = await render({ audience: { ...AUDIENCE, followers: 0, devices: 0 } });
    expect(buttonByText(fixture.nativeElement, 'Pushmelding versturen').disabled).toBe(true);
    expect(text(fixture.nativeElement)).toContain('Er is nog niemand');
  });

  it('waarschuwt in een testomgeving dat er niets op telefoons aankomt', async () => {
    const fixture = await render({ audience: { ...AUDIENCE, simulated: true } });
    expect(text(fixture.nativeElement)).toContain('alleen gelogd');
  });

  it('meldt een dubbele verzending in begrijpelijke taal', async () => {
    ask.mockResolvedValue(true);
    const fixture = await render();
    buttonByText(fixture.nativeElement, 'Pushmelding versturen').click();
    await fixture.whenStable();
    const { body, options } = apiError('PUSH_ALREADY_SENT', 409);
    await respond(SEND, body, options);
    await fixture.whenStable();
    expect(text(fixture.nativeElement)).toContain('al een pushmelding verstuurd');
  });
});
