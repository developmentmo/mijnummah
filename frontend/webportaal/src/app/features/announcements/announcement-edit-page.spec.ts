import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { Confirm } from '../../shared/ui/confirm-dialog';
import {
  MOSQUE,
  V1,
  anAnnouncement,
  apiError,
  buttonByText,
  configureTesting,
  http,
  respond,
  respondPushPanel,
  selectMosque,
  spyOnRouter,
  text,
} from '../../testing/harness';
import { AnnouncementEditPage } from './announcement-edit-page';

const BASE = `${V1}/admin/mosques/${MOSQUE.id}/announcements`;

describe('AnnouncementEditPage', () => {
  const ask = vi.fn();

  beforeEach(async () => {
    ask.mockReset();
    sessionStorage.clear();
    configureTesting([{ provide: Confirm, useValue: { ask } }]);
    spyOnRouter();
    await selectMosque();
  });

  afterEach(() => http().verify());

  async function open(announcement = anAnnouncement()) {
    const fixture = TestBed.createComponent(AnnouncementEditPage);
    fixture.componentRef.setInput('announcementId', announcement.id);
    await fixture.whenStable();
    await respond(`${BASE}/${announcement.id}`, announcement);
    await fixture.whenStable();
    await respondPushPanel();
    await fixture.whenStable();
    return fixture;
  }

  it('maakt het verschil tussen publiceren en een pushmelding duidelijk', async () => {
    const fixture = await open();
    const panel = text(fixture.nativeElement.querySelector('aside'));
    expect(panel).toContain('Publiceren verstuurt nooit een pushmelding');
    // Een concept kan nog niet als pushmelding worden verstuurd.
    expect(panel).toContain('Publiceer de aankondiging eerst');
    expect(() => buttonByText(fixture.nativeElement, 'Pushmelding versturen')).toThrow();
  });

  it('biedt na publiceren een aparte pushmelding aan', async () => {
    const fixture = await open(
      anAnnouncement({ status: 'PUBLISHED', publishedAt: '2026-10-11T10:00:00Z' }),
    );
    expect(buttonByText(fixture.nativeElement, 'Pushmelding versturen')).toBeTruthy();
    expect(text(fixture.nativeElement.querySelector('aside'))).toContain(
      '12 volgers op 15 apparaten',
    );
  });

  it('vraagt bevestiging met de expliciete melding dat er geen push gaat', async () => {
    ask.mockResolvedValue(true);
    const fixture = await open();
    buttonByText(fixture.nativeElement, 'Publiceren in de app').click();
    await fixture.whenStable();
    expect(ask).toHaveBeenCalledWith(
      expect.objectContaining({
        confirmLabel: 'Publiceren in de app',
        note: expect.stringContaining('géén pushmelding'),
      }),
    );
    await respond(
      `${BASE}/a-1/publish`,
      anAnnouncement({ status: 'PUBLISHED', publishedAt: '2026-10-11T10:00:00Z' }),
    );
    await fixture.whenStable();
    expect(text(fixture.nativeElement.querySelector('aside'))).toContain('Gepubliceerd');
  });

  it('legt uit dat een verlopen aankondiging niet gepubliceerd kan worden', async () => {
    ask.mockResolvedValue(true);
    const fixture = await open();
    buttonByText(fixture.nativeElement, 'Publiceren in de app').click();
    const { body, options } = apiError('INVALID_ANNOUNCEMENT', 400);
    await respond(`${BASE}/a-1/publish`, body, options);
    await fixture.whenStable();
    expect(text(fixture.nativeElement)).toContain('De vervaldatum ligt in het verleden.');
  });

  it('toont een gepubliceerde, verlopen aankondiging als verlopen', async () => {
    const fixture = await open(
      anAnnouncement({ status: 'PUBLISHED', expired: true, expiresAt: '2026-10-01T21:59:00Z' }),
    );
    expect(text(fixture.nativeElement.querySelector('mu-status-badge'))).toBe('Verlopen');
  });

  it('zet een vervaldatum zonder tijd op het einde van de dag in Amsterdam', async () => {
    const fixture = TestBed.createComponent(AnnouncementEditPage);
    await fixture.whenStable();
    const form = (fixture.componentInstance as unknown as { form: AnnouncementEditPage['form'] })
      .form;
    form.patchValue({ title: 'Titel', body: 'Tekst', expiresDate: '2026-12-31' });
    buttonByText(fixture.nativeElement, 'Opslaan als concept').click();
    const created = await respond(BASE, anAnnouncement({ id: 'a-2' }));
    expect(created.request.body).toEqual({
      title: 'Titel',
      body: 'Tekst',
      featured: false,
      expiresAt: '2026-12-31T22:59:00Z',
    });
  });
});
